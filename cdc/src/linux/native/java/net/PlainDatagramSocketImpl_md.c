/*
 * @(#)PlainDatagramSocketImpl_md.c	1.31 06/05/03
 *
 * Copyright  1990-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation. 
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt). 
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA 
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions. 
 */

#include <errno.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>

#ifdef __linux__
#include <sys/utsname.h>
#include <netinet/ip.h>

#define IPV6_MULTICAST_IF 17
#ifndef SO_BSDCOMPAT
#define SO_BSDCOMPAT  14
#endif
#endif

#ifndef IPTOS_TOS_MASK
#define IPTOS_TOS_MASK 0x1e
#endif
#ifndef IPTOS_PREC_MASK
#define IPTOS_PREC_MASK 0xe0
#endif 

#include "jvm.h"
#include "jni_util.h"
#include "net_util.h"

#include "java_net_InetAddress.h"
#include "java_net_Inet4Address.h"
#include "java_net_Inet6Address.h"
#include "java_net_SocketOptions.h"
#include "java_net_PlainDatagramSocketImpl.h"
#include "java_net_NetworkInterface.h"
/************************************************************************
 * PlainDatagramSocketImpl
 */

#include "jni_statics.h"

static jboolean isOldKernel;

/*
 * Returns a java.lang.Integer based on 'i'
 */
static jobject createInteger(JNIEnv *env, int i) {
    static jclass i_class;
    static jmethodID i_ctrID;

    if (i_class == NULL) {
 	jclass c = (*env)->FindClass(env, "java/lang/Integer");
	CHECK_NULL_RETURN(c, NULL);
        i_ctrID = (*env)->GetMethodID(env, c, "<init>", "(I)V");
	CHECK_NULL_RETURN(i_ctrID, NULL);
	i_class = (*env)->NewGlobalRef(env, c);
	CHECK_NULL_RETURN(i_class, NULL);
    }

    return ( (*env)->NewObject(env, i_class, i_ctrID, i) );
}

/*
 * Returns a java.lang.Boolean based on 'b'
 */
static jobject createBoolean(JNIEnv *env, int b) {
    static jclass b_class;
    static jmethodID b_ctrID;

    if (b_class == NULL) {
        jclass c = (*env)->FindClass(env, "java/lang/Boolean");
	CHECK_NULL_RETURN(c, NULL);
        b_ctrID = (*env)->GetMethodID(env, c, "<init>", "(Z)V");
	CHECK_NULL_RETURN(b_ctrID, NULL);
        b_class = (*env)->NewGlobalRef(env, c);
	CHECK_NULL_RETURN(b_class, NULL);
    }

    return( (*env)->NewObject(env, b_class, b_ctrID, (jboolean)(b!=0)) );
}


/*
 * Returns the fd for a PlainDatagramSocketImpl or -1
 * if closed.
 */
static int getFD(JNIEnv *env, jobject this) {
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    if (fdObj == NULL) {
        return -1;
    }
    return (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
}

/* Do we need this any longer???
 * the maximum buffer size. Used for setting
 * SendBufferSize and ReceiveBufferSize.
 */
static const int max_buffer_size = 64 * 1024;

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_init(JNIEnv *env, jclass cls) {

    struct utsname sysinfo;

    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID) 
    	= (*env)->GetFieldID(env, cls, "fd", "Ljava/io/FileDescriptor;");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_timeoutID) 
    	= (*env)->GetFieldID(env, cls, "timeout", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_timeoutID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_trafficClassID) 
        = (*env)->GetFieldID(env, cls, "trafficClass", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_trafficClassID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_localPortID) 
    	= (*env)->GetFieldID(env, cls, "localPort", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_localPortID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connected)
        = (*env)->GetFieldID(env, cls, "connected", "Z");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connected));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connectedAddress)
        = (*env)->GetFieldID(env, cls, "connectedAddress", "Ljava/net/InetAddress;");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connectedAddress));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connectedPort)
        = (*env)->GetFieldID(env, cls, "connectedPort", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connectedPort));

    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID) 
    	= NET_GetFileDescriptorID(env);
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));

    Java_java_net_InetAddress_init(env, 0);
    Java_java_net_Inet4Address_init(env, 0);
    Java_java_net_Inet6Address_init(env, 0);
    Java_java_net_NetworkInterface_init(env, 0);

    /*
     * We need to determine if this is a 2.2 kernel.
     */
    if (uname(&sysinfo) == 0) {
        sysinfo.release[3] = '\0';
        isOldKernel = (strcmp(sysinfo.release, "2.2") == 0);
    } else {
	/*
 	 * uname failed - move to plan B and examine /proc/version
	 * If this fails assume that /proc has changed and that
	 * this must be new /proc format and hence new kernel.
	 */
 	FILE *fP;
	isOldKernel = JNI_FALSE;
	if ((fP = fopen("/proc/version", "r")) != NULL) {
	    char ver[25];
	    if (fgets(ver, sizeof(ver), fP) != NULL) {
		isOldKernel = (strstr(ver, "2.2.") != NULL);
	    }
	    fclose(fP);
	}
    }

#ifdef AF_INET6
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_multicastInterfaceID ) =
        (*env)->GetFieldID(env, cls, "multicastInterface", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_multicastInterfaceID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_loopbackID) =
        (*env)->GetFieldID(env, cls, "loopbackMode", "Z");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_loopbackID));
    JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_ttlID) = 
        (*env)->GetFieldID(env, cls, "ttl", "I");
    CHECK_NULL(JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_ttlID));
#endif


}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    bind
 * Signature: (ILjava/net/InetAddress;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_bind(JNIEnv *env, jobject this,
					   jint localport, jobject iaObj) {
    /* fdObj is the FileDescriptor field on this */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    /* fd is an int field on fdObj */
    int fd;
    int len = 0;
    SOCKADDR him;

    if (IS_NULL(fdObj)) {
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"Socket closed");
	return;
    } else {
	fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    }
  
    if (IS_NULL(iaObj)) {
	JNU_ThrowNullPointerException(env, "iaObj is null.");
	return;
    }
    
    /* bind - pick a port number for local addr*/
    NET_InetAddressToSockaddr(env, iaObj, localport, (struct sockaddr *)&him, &len);

    if (NET_Bind(fd, (struct sockaddr *)&him, len) < 0)  {
	if (errno == EADDRINUSE || errno == EADDRNOTAVAIL ||    
	    errno == EPERM || errno == EACCES) {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "BindException",
                                         "Bind failed");
	} else {
	    NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                         "Bind failed");
	}
	return;
    } 
    
    /* intialize the local port */
    if (localport == 0) {
	/* Now that we're a connected socket, let's extract the port number
	 * that the system chose for us and store it in the Socket object.
  	 */
	if (JVM_GetSockName(fd, (struct sockaddr *)&him, &len) == -1) {
	    NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                            "Error getting socket name");
	    return;
	}

	localport = NET_GetPortFromSockaddr((struct sockaddr *)&him);

	(*env)->SetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_localPortID), localport);
    } else {
	(*env)->SetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_localPortID), localport);
    }
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    connect0
 * Signature: (Ljava/net/InetAddress;I)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_connect0(JNIEnv *env, jobject this,
					       jobject address, jint port) {
    /*The object's field */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    /* The fdObj'fd */
    jint fd;
    /* The packetAddress address, family and port */
    SOCKADDR rmtaddr;
    int len = 0; 

    if (IS_NULL(fdObj)) {
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"Socket closed");
	return;
    }
    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    
    if (IS_NULL(address)) {
	JNU_ThrowNullPointerException(env, "address");
	return;
    }

    NET_InetAddressToSockaddr(env, address, port, (struct sockaddr *)&rmtaddr, &len);

    if (isOldKernel) {
	int t = 0;
        setsockopt(fd, SOL_SOCKET, SO_BSDCOMPAT, (char*) &t, sizeof(int));
    } else 

    { 
	if (JVM_Connect(fd, (struct sockaddr *)&rmtaddr, len) == -1) {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                            "Connection problem encountered: No network connection detected.");
	    return;
	}
    }
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    disconnect0
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_disconnect0(JNIEnv *env, jobject this) {
    /* The object's field */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    /* The fdObj'fd */
    jint fd;

    SOCKADDR addr;
    int len;

    if (IS_NULL(fdObj)) {
        return;
    } 
    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));

    if (isOldKernel) {
	int t = 1;
        setsockopt(fd, SOL_SOCKET, SO_BSDCOMPAT, (char*) &t, sizeof(int));
    } else {
        memset(&addr, 0, sizeof(addr));
#ifdef AF_INET6
        if (ipv6_available()) {
            struct sockaddr_in6 *him6 = (struct sockaddr_in6 *)&addr;
            him6->sin6_family = AF_UNSPEC;
            len = sizeof(struct sockaddr_in6);
        } else
#endif
        {
            struct sockaddr_in *him4 = (struct sockaddr_in*)&addr;
            him4->sin_family = AF_UNSPEC;
            len = sizeof(struct sockaddr_in);
        }
        JVM_Connect(fd, (struct sockaddr *)&addr, len);
    }
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    send
 * Signature: (Ljava/net/DatagramPacket;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_send(JNIEnv *env, jobject this,
					   jobject packet) {

    char BUF[MAX_BUFFER_LEN];
    char *fullPacket = NULL;
    int ret, mallocedPacket = JNI_FALSE;
    /* The object's field */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint trafficClass = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl,  pdsi_trafficClassID));

    jbyteArray packetBuffer;
    jobject packetAddress;
    jint packetBufferOffset, packetBufferLen, packetPort;
    jboolean connected;

    /* The fdObj'fd */
    jint fd;

    SOCKADDR rmtaddr, *rmtaddrP=&rmtaddr;
    int len;

    if (IS_NULL(fdObj)) {
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"Socket closed");
	return;
    }
    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));

    if (IS_NULL(packet)) {
	JNU_ThrowNullPointerException(env, "packet");
	return;
    }

    connected = (*env)->GetBooleanField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connected));

    packetBuffer = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_bufID));
    packetAddress = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_addressID));
    if (IS_NULL(packetBuffer) || IS_NULL(packetAddress)) {
	JNU_ThrowNullPointerException(env, "null buffer || null address");
	return;
    }

    packetBufferOffset = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID));
    packetBufferLen = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID));

    if (connected && !isOldKernel) {
	/* arg to NET_Sendto () null in this case */
	len = 0;
	rmtaddrP = 0;
    } else {
	packetPort = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_portID));
	NET_InetAddressToSockaddr(env, packetAddress, packetPort, (struct sockaddr *)&rmtaddr, &len);
    }

    if (packetBufferLen > MAX_BUFFER_LEN) {
	/* When JNI-ifying the JDK's IO routines, we turned
	 * read's and write's of byte arrays of size greater
	 * than 2048 bytes into several operations of size 2048.
	 * This saves a malloc()/memcpy()/free() for big
	 * buffers.  This is OK for file IO and TCP, but that
	 * strategy violates the semantics of a datagram protocol.
	 * (one big send) != (several smaller sends).  So here
	 * we *must* alloc the buffer.  Note it needn't be bigger
	 * than 65,536 (0xFFFF) the max size of an IP packet.
	 * Anything bigger should be truncated anyway.
	 *
	 * We may want to use a smarter allocation scheme at some
	 * point.
	 */
	if (packetBufferLen > MAX_PACKET_LEN) {
	    packetBufferLen = MAX_PACKET_LEN;
	}
	fullPacket = (char *)malloc(packetBufferLen);

	if (!fullPacket) {
	    JNU_ThrowOutOfMemoryError(env, "heap allocation failed");
	    return;
	} else {
	    mallocedPacket = JNI_TRUE;
	}
    } else {
	fullPacket = &(BUF[0]);
    }

    (*env)->GetByteArrayRegion(env, packetBuffer, packetBufferOffset, packetBufferLen,
			       (jbyte *)fullPacket);
#ifdef AF_INET6
    if (trafficClass != 0 && ipv6_available()) {
	NET_SetTrafficClass((struct sockaddr *)&rmtaddr, trafficClass);
    }
#endif /* AF_INET6 */


    /*
     * Send the datagram. 
     *
     * If we are connected it's possible that sendto will return
     * ECONNREFUSED indicating that an ICMP port unreachable has
     * received.
     */
    ret = NET_SendTo(fd, fullPacket, packetBufferLen, 0, 
		     (struct sockaddr *)rmtaddrP, len);

    if (ret < 0) {
	switch (ret) {
	    case JVM_IO_ERR :
		if (errno == ECONNREFUSED) {
		    JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException",
                            "ICMP Port Unreachable");
		} else {
		    NET_ThrowByNameWithLastError(env, "java/io/IOException", "sendto failed");
		}
		break;

	    case JVM_IO_INTR:
	        JNU_ThrowByName(env, "java/io/InterruptedIOException",
			        "operation interrupted");
		break;
	}
    }

    if (mallocedPacket) {
	free(fullPacket);
    }
    return;
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    peek
 * Signature: (Ljava/net/InetAddress;)I
 */
JNIEXPORT jint JNICALL
Java_java_net_PlainDatagramSocketImpl_peek(JNIEnv *env, jobject this,
					   jobject addressObj) {

    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint timeout = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_timeoutID));
    jint fd;
    ssize_t n;
    SOCKADDR remote_addr;
    int len;
    char buf[1];
    jint family;
    jobject iaObj;
    int port;
    if (IS_NULL(fdObj)) {
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
	return -1;
    } else {
	fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    }
    if (IS_NULL(addressObj)) {
	JNU_ThrowNullPointerException(env, "Null address in peek()");
    }
    if (timeout) {
	int ret = NET_Timeout(fd, timeout);
        if (ret == 0) {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketTimeoutException",
			    "Peek timed out");
	    return ret;
	} else if (ret == JVM_IO_ERR) {
	    if (errno == EBADF) {
                 JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
            } else {
                 NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Peek failed"); 
            } 
	    return ret;
	} else if (ret == JVM_IO_INTR) {
	    JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException",
			    "operation interrupted");
	    return ret;	/* WARNING: SHOULD WE REALLY RETURN -2??? */
	}
    }

    len = SOCKADDR_LEN;
    n = NET_RecvFrom(fd, buf, 1, MSG_PEEK,
		     (struct sockaddr *)&remote_addr, &len);

    if (n == JVM_IO_ERR) {
        if (errno == ECONNREFUSED) {
	    JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException",
			    "ICMP Port Unreachable");
	} else {
	    if (errno == EBADF) {
                 JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
            } else {
                 NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Peek failed");
            }
	}
        return 0;
    } else if (n == JVM_IO_INTR) {
	JNU_ThrowByName(env, "java/io/InterruptedIOException", 0);
        return 0;
    }

    iaObj = NET_SockaddrToInetAddress(env, (struct sockaddr *)&remote_addr, &port);
    family = (*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_familyID)) == IPv4? AF_INET : AF_INET6;
    if (family == AF_INET) { /* this api can't handle IPV6 addresses */
	int address = (*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID));
	(*env)->SetIntField(env, addressObj, JNI_STATIC(java_net_InetAddress, ia_addressID), address);
    }
    return port;
}

JNIEXPORT jint JNICALL
Java_java_net_PlainDatagramSocketImpl_peekData(JNIEnv *env, jobject this,
					   jobject packet) {

    char BUF[MAX_BUFFER_LEN];
    char *fullPacket = NULL;
    int mallocedPacket = JNI_FALSE;
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint timeout = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_timeoutID));

    jbyteArray packetBuffer;
    jint packetBufferOffset, packetBufferLen;

    int fd;

    int n;
    SOCKADDR remote_addr;
    int len;
    int port;

    if (IS_NULL(fdObj)) {
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"Socket closed");
	return -1;
    }

    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));

    if (IS_NULL(packet)) {
	JNU_ThrowNullPointerException(env, "packet");
	return -1;
    }

    packetBuffer = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_bufID));
    if (IS_NULL(packetBuffer)) {
	JNU_ThrowNullPointerException(env, "packet buffer");
        return -1;
    }
    packetBufferOffset = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID));
    packetBufferLen = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID));
    if (timeout) {
	int ret = NET_Timeout(fd, timeout);
        if (ret == 0) {
            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketTimeoutException",
			    "Receive timed out");
	    return -1;
	} else if (ret == JVM_IO_ERR) {
            if (errno == EBADF) {
                JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
            } else {
                NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Receive failed");
            }
	    return -1;
	} else if (ret == JVM_IO_INTR) {
	    JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException",
			    "operation interrupted");
	    return -1;
	}
    }

    if (packetBufferLen > MAX_BUFFER_LEN) {

	/* When JNI-ifying the JDK's IO routines, we turned
	 * read's and write's of byte arrays of size greater
	 * than 2048 bytes into several operations of size 2048.
	 * This saves a malloc()/memcpy()/free() for big
	 * buffers.  This is OK for file IO and TCP, but that
	 * strategy violates the semantics of a datagram protocol.
	 * (one big send) != (several smaller sends).  So here
	 * we *must* alloc the buffer.  Note it needn't be bigger
	 * than 65,536 (0xFFFF) the max size of an IP packet.
	 * anything bigger is truncated anyway.
	 *
	 * We may want to use a smarter allocation scheme at some
	 * point.
	 */
	if (packetBufferLen > MAX_PACKET_LEN) {
	    packetBufferLen = MAX_PACKET_LEN;
	}
	fullPacket = (char *)malloc(packetBufferLen);

	if (!fullPacket) {
	    JNU_ThrowOutOfMemoryError(env, "heap allocation failed");
	    return -1;
	} else {
	    mallocedPacket = JNI_TRUE;
	}
    } else {
	fullPacket = &(BUF[0]);
    }

    len = SOCKADDR_LEN;
    n = NET_RecvFrom(fd, fullPacket, packetBufferLen, MSG_PEEK,
		     (struct sockaddr *)&remote_addr, &len);
    /* truncate the data if the packet's length is too small */
    if (n > packetBufferLen) {
	n = packetBufferLen;
    }
    if (n == JVM_IO_ERR) {

#ifdef __solaris__ 
        if (errno == ECONNREFUSED) {
            int orig_errno = errno;
	    (void) recv(fd, fullPacket, 1, 0);
            errno = orig_errno;
        }
#endif
	if (errno == ECONNREFUSED) {
	    JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException",
			    "ICMP Port Unreachable");
	} else {
	    if (errno == EBADF) {
                 JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
            } else {
                 NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Receive failed");
            }
	}
	(*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID), 0);
	(*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), 0);
    } else if (n == JVM_IO_INTR) {
	JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException",
			"operation interrupted");
	(*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID), 0);
	(*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), 0);
    } else {
	/*
	 * success - fill in received address...
	 *
	 * REMIND: Fill in an int on the packet, and create inetadd
	 * object in Java, as a performance improvement. Also
	 * construct the inetadd object lazily.
	 */

        jobject packetAddress;

        /*
         * Check if there is an InetAddress already associated with this
         * packet. If so we check if it is the same source address. We
         * can't update any existing InetAddress because it is immutable
         */
        packetAddress = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_addressID));
        if (packetAddress != NULL) {
            if (!NET_SockaddrEqualsInetAddress(env, (struct sockaddr *)&remote_addr, packetAddress)) {
                /* force a new InetAddress to be created */
                packetAddress = NULL;
            }
        }
	if (packetAddress == NULL) {
	    packetAddress = NET_SockaddrToInetAddress(env, (struct sockaddr *)&remote_addr, &port);
	    /* stuff the new Inetaddress in the packet */
	    (*env)->SetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_addressID), packetAddress);
	} else {
 	    /* only get the new port number */
 	    port = NET_GetPortFromSockaddr((struct sockaddr *)&remote_addr);
  	}
	/* and fill in the data, remote address/port and such */
	(*env)->SetByteArrayRegion(env, packetBuffer, packetBufferOffset, n,
				   (jbyte *)fullPacket);
	(*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_portID), port);
	(*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), n);
    }

    if (mallocedPacket) {
	free(fullPacket);
    }
    return port;
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    receive
 * Signature: (Ljava/net/DatagramPacket;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_receive(JNIEnv *env, jobject this,
					      jobject packet) {

    char BUF[MAX_BUFFER_LEN];
    char *fullPacket = NULL;
    int mallocedPacket = JNI_FALSE;
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint timeout = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_timeoutID));

    jbyteArray packetBuffer;
    jint packetBufferOffset, packetBufferLen;

    int fd;

    int n;
    SOCKADDR remote_addr;
    int len;
    jboolean retry;
    jboolean connected = JNI_FALSE;
    jobject connectedAddress = 0;
    jint connectedPort = 0;
    jlong prevTime = 0;

    if (IS_NULL(fdObj)) {
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"Socket closed");
	return;
    }

    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));

    if (IS_NULL(packet)) {
	JNU_ThrowNullPointerException(env, "packet");
	return;
    }

    packetBuffer = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_bufID));
    if (IS_NULL(packetBuffer)) {
	JNU_ThrowNullPointerException(env, "packet buffer");
        return;
    }
    packetBufferOffset = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID));
    packetBufferLen = (*env)->GetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_bufLengthID));

    if (packetBufferLen > MAX_BUFFER_LEN) {

	/* When JNI-ifying the JDK's IO routines, we turned
	 * read's and write's of byte arrays of size greater
	 * than 2048 bytes into several operations of size 2048.
	 * This saves a malloc()/memcpy()/free() for big
	 * buffers.  This is OK for file IO and TCP, but that
	 * strategy violates the semantics of a datagram protocol.
	 * (one big send) != (several smaller sends).  So here
	 * we *must* alloc the buffer.  Note it needn't be bigger
	 * than 65,536 (0xFFFF) the max size of an IP packet.
	 * anything bigger is truncated anyway.
	 *
	 * We may want to use a smarter allocation scheme at some
	 * point.
	 */
	if (packetBufferLen > MAX_PACKET_LEN) {
	    packetBufferLen = MAX_PACKET_LEN;
	}
	fullPacket = (char *)malloc(packetBufferLen);

	if (!fullPacket) {
	    JNU_ThrowOutOfMemoryError(env, "heap allocation failed");
	    return;
	} else {
	    mallocedPacket = JNI_TRUE;
	}
    } else {
	fullPacket = &(BUF[0]);
    }

    /*
     * On Linux with the 2.2 kernel we simulate connected datagrams by
     * discarding packets
     */
    if (isOldKernel) {
        connected = (*env)->GetBooleanField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connected));
        if (connected) {
            connectedAddress = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connectedAddress));
            connectedPort = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_connectedPort));

	    if (timeout) {
		prevTime = JVM_CurrentTimeMillis(env, 0);
	    }
        }
    }

    do {
	retry = JNI_FALSE;

	if (timeout) {
            int ret = NET_Timeout(fd, timeout);
            if (ret <= 0) {
                if (ret == 0) {
                    JNU_ThrowByName(env, JNU_JAVANETPKG "SocketTimeoutException",
                                    "Receive timed out");
                } else if (ret == JVM_IO_ERR) {
                    if (errno == EBADF) {
                         JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
                     } else {
                         NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Receive failed");
                     }
                } else if (ret == JVM_IO_INTR) {
                    JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException",
                                    "operation interrupted");
                }

                if (mallocedPacket) {
                    free(fullPacket);
                }

                return;
            }
        }

	/*
	 * Security Note: For Linux 2.2 with connected datagrams ensure that 
	 * you receive into the stack/heap allocated buffer - do not attempt
	 * to receive directly into DatagramPacket's byte array.
	 * (ie: if the virtual machine support pinning don't use 
	 * GetByteArrayElements or a JNI critical section and receive
	 * directly into the byte array)
	 */
        len = SOCKADDR_LEN;
        n = NET_RecvFrom(fd, fullPacket, packetBufferLen, 0,
                         (struct sockaddr *)&remote_addr, &len);
        /* truncate the data if the packet's length is too small */
        if (n > packetBufferLen) {
            n = packetBufferLen;
        }
        if (n == JVM_IO_ERR) {
            if (errno == ECONNREFUSED) {
                JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException",
                                "ICMP Port Unreachable");
            } else {
                if (errno == EBADF) { 
                     JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Socket closed");
                 } else {
                     NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Receive failed");
                 }
            }
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID), 0);
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), 0);
        } else if (n == JVM_IO_INTR) {
            JNU_ThrowByName(env, JNU_JAVAIOPKG "InterruptedIOException",
                            "operation interrupted");
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_offsetID), 0);
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), 0);
        } else {
            int port;
            jobject packetAddress;

            /*
             * If we are connected then we know that the datagram that we have
             * received is from the address that we are connected too. However
             * on Linux with 2.2 kernel we have to simulate this behaviour by
             * discarding any datagrams that aren't from the connected address.
             */
            if (isOldKernel && connected) {

                if (NET_GetPortFromSockaddr((struct sockaddr *)&remote_addr) != connectedPort ||
                    !NET_SockaddrEqualsInetAddress(env, (struct sockaddr *)&remote_addr, connectedAddress)) {

                    /*
                     * Discard the datagram as it's not from the connected
                     * address
                     */
                    retry = JNI_TRUE;

                    /*
                     * Adjust timeout if necessary to ensure that we adhere to
                     * timeout semantics.
                     */
                    if (timeout) {
                        jlong newTime = JVM_CurrentTimeMillis(env, 0);
                        timeout -= (newTime - prevTime);
                        if (timeout <= 0) {
                            JNU_ThrowByName(env, JNU_JAVANETPKG "SocketTimeoutException",
                                    "Receive timed out");
                            if (mallocedPacket) {
                                free(fullPacket);
                            }
                            return;
			}
			prevTime = newTime;
                    }

                    continue;
                }
            }

            /*
             * success - fill in received address...
             *
             * REMIND: Fill in an int on the packet, and create inetadd
             * object in Java, as a performance improvement. Also
             * construct the inetadd object lazily.
             */

            /*
             * Check if there is an InetAddress already associated with this
             * packet. If so we check if it is the same source address. We
             * can't update any existing InetAddress because it is immutable
             */
            packetAddress = (*env)->GetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_addressID));
            if (packetAddress != NULL) {
                if (!NET_SockaddrEqualsInetAddress(env, (struct sockaddr *)&remote_addr, packetAddress)) {
                    /* force a new InetAddress to be created */
                    packetAddress = NULL;
                }
            }
            if (packetAddress == NULL) {
                packetAddress = NET_SockaddrToInetAddress(env, (struct sockaddr *)&remote_addr, &port);
                /* stuff the new Inetaddress in the packet */
                (*env)->SetObjectField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_addressID), packetAddress);
            } else {
                /* only get the new port number */
                port = NET_GetPortFromSockaddr((struct sockaddr *)&remote_addr);
            }
            /* and fill in the data, remote address/port and such */
            (*env)->SetByteArrayRegion(env, packetBuffer, packetBufferOffset, n,
                                       (jbyte *)fullPacket);
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_portID), port);
            (*env)->SetIntField(env, packet, JNI_STATIC(java_net_DatagramPacket, dp_lengthID), n);
        }

    } while (retry);

    if (mallocedPacket) {
	free(fullPacket);
    }
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    datagramSocketCreate
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_datagramSocketCreate(JNIEnv *env,
							   jobject this) {
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    int fd;

    int t = 1;

    if (IS_NULL(fdObj)) {
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"Socket closed");
	return;
    } else {
#ifdef AF_INET6
	if (ipv6_available()) {	
	    fd =  JVM_Socket(AF_INET6, SOCK_DGRAM, 0);
	} else 
#endif /* AF_INET6 */
	    {
		fd =  JVM_Socket(AF_INET, SOCK_DGRAM, 0);
	    }
    }
    if (fd == JVM_IO_ERR) {
	NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                     "Error creating socket");
	return;
    }

    setsockopt(fd, SOL_SOCKET, SO_BROADCAST, (char*) &t, sizeof(int));

    if (isOldKernel) {
        setsockopt(fd, SOL_SOCKET, SO_BSDCOMPAT, (char*) &t, sizeof(int)); 
    }

#ifdef AF_INET6
    /*
     * On Linux for IPv6 sockets we must set the hop limit
     * to 1 to be compatible with default ttl of 1 for IPv4 sockets.
     */
    if (ipv6_available()) {
        int ttl = 1;
	setsockopt(fd, IPPROTO_IPV6, IPV6_MULTICAST_HOPS, (char *)&ttl, 
		   sizeof(ttl));

	if (isOldKernel) {
            (*env)->SetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_ttlID), ttl);
        }
    }
#endif  

    (*env)->SetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID), fd);
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    datagramSocketClose
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_datagramSocketClose(JNIEnv *env,
							  jobject this) {
    /*
     * REMIND: PUT A LOCK AROUND THIS CODE
     */
    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    int fd;

    if (IS_NULL(fdObj)) {
	return;
    }
    fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    if (fd == -1) {
	return;
    }
    (*env)->SetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID), -1);
    NET_SocketClose(fd);
}


/*
 * Sets the multicast interface. 
 *
 * SocketOptions.IP_MULTICAST_IF :-
 *	value is a InetAddress
 *	IPv4:	set outgoing multicast interface using 
 * 		IPPROTO_IP/IP_MULTICAST_IF
 *	IPv6:	Get the index of the interface to which the
 *		InetAddress is bound
 *		Set outgoing multicast interface using
 *		IPPROTO_IPV6/IPV6_MULTICAST_IF
 *		On Linux 2.2 record interface index as can't
 *		query the multicast interface.
 *
 * SockOptions.IF_MULTICAST_IF2 :-
 *	value is a NetworkInterface 
 *	IPv4:	Obtain IP address bound to network interface
 *		(NetworkInterface.addres[0])
 *		set outgoing multicast interface using 
 *              IPPROTO_IP/IP_MULTICAST_IF
 *	IPv6:	Obtain NetworkInterface.index
 *		Set outgoing multicast interface using
 *              IPPROTO_IPV6/IPV6_MULTICAST_IF
 *              On Linux 2.2 record interface index as can't
 *              query the multicast interface. 
 *
 */
static void setMulticastInterface(JNIEnv *env, jobject this, int fd,
				  jint opt, jobject value)
{
    if (opt == java_net_SocketOptions_IP_MULTICAST_IF) {
	/*
  	 * value is an InetAddress.
	 * On IPv4 system use IP_MULTICAST_IF socket option
	 * On IPv6 system get the NetworkInterface that this IP
	 * address is bound too and use the IPV6_MULTICAST_IF 
	 * option instead of IP_MULTICAST_IF
	 */
#ifdef AF_INET6
	if (ipv6_available()) {
	    static jclass ni_class;
	    if (ni_class == NULL) {
 		jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
		CHECK_NULL(c);
		ni_class = (*env)->NewGlobalRef(env, c);
		CHECK_NULL(ni_class);
	    }

	    value = Java_java_net_NetworkInterface_getByInetAddress0(env, ni_class, value);
	    if (value == NULL) {
		if (!(*env)->ExceptionOccurred(env)) {
		    JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
	                 "bad argument for IP_MULTICAST_IF"
			 ": address not bound to any interface");
		}
		return;
	    }
	    opt = java_net_SocketOptions_IP_MULTICAST_IF2;
	} else
#endif /* AF_INET6 */
	{
	    static jfieldID ia_addressID;
	    struct in_addr in;

	    if (ia_addressID == NULL) {
		jclass c = (*env)->FindClass(env,"java/net/InetAddress");
		CHECK_NULL(c);
		ia_addressID = (*env)->GetFieldID(env, c, "address", "I");
		CHECK_NULL(ia_addressID);
	    }
	    
            in.s_addr = htonl( (*env)->GetIntField(env, value, ia_addressID) );

            if (JVM_SetSockOpt(fd, IPPROTO_IP, IP_MULTICAST_IF,
                               (const char*)&in, sizeof(in)) < 0) {
                NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                 "Error setting socket option");
	    }
	    return;
	}
    }

    if (opt == java_net_SocketOptions_IP_MULTICAST_IF2) {
	/*
	 * value is a NetworkInterface.
	 * On IPv6 system get the index of the interface and use the
         * IPV6_MULTICAST_IF socket option
	 * On IPv4 system extract addr[0] and use the IP_MULTICAST_IF
         * option.
	 */
#ifdef AF_INET6
        if (ipv6_available()) {
	    static jfieldID ni_indexID;
	    int index;

	    if (ni_indexID == NULL) {
		jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
		CHECK_NULL(c);
		ni_indexID = (*env)->GetFieldID(env, c, "index", "I");
		CHECK_NULL(ni_indexID);
	    }
            index = (*env)->GetIntField(env, value, ni_indexID);

            if (JVM_SetSockOpt(fd, IPPROTO_IPV6, IPV6_MULTICAST_IF,
			       (const char*)&index, sizeof(index)) < 0) {
		if (errno == EINVAL && index > 0) {
		    JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"IPV6_MULTICAST_IF failed (interface has IPv4 "
			"address only?)");
		} else {
	            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                   "Error setting socket option");
	  	}
		return;
	    }
	    /*
	     * Linux 2.2 kernel doesn't support IPV6_MULTICAST_IF socket
	     * option so record index for later retrival.
	     */
	    if (isOldKernel) {
	        (*env)->SetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_multicastInterfaceID), (jint)index);
	    }
	    return;
        } else
#endif /* AF_INET6 */
        {
	    static jfieldID ni_addrsID;
	    static jfieldID ia_addressID;
	    struct in_addr in;
	    jobjectArray addrArray;
	    jsize len;
	    jobject addr;

	    if (ni_addrsID == NULL) {
		jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
		CHECK_NULL(c);
		ni_addrsID = (*env)->GetFieldID(env, c, "addrs", 
						"[Ljava/net/InetAddress;");
		CHECK_NULL(ni_addrsID);
	 	c = (*env)->FindClass(env,"java/net/InetAddress");
	        CHECK_NULL(c);
                ia_addressID = (*env)->GetFieldID(env, c, "address", "I");
		CHECK_NULL(ia_addressID);
	    }

	    addrArray = (*env)->GetObjectField(env, value, ni_addrsID);
	    len = (*env)->GetArrayLength(env, addrArray);

	    /*
 	     * Check that there is at least one address bound to this
	     * interface.
	     */
	    if (len < 1) {
		JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
		    "bad argument for IP_MULTICAST_IF2: No IP addresses bound to interface");
		return;
	    }
	   
	    addr = (*env)->GetObjectArrayElement(env, addrArray, 0);
	    in.s_addr = htonl((*env)->GetIntField(env, addr, ia_addressID));

	    if (JVM_SetSockOpt(fd, IPPROTO_IP, IP_MULTICAST_IF,
			       (const char*)&in, sizeof(in)) < 0) {
	        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                               "Error setting socket option");
	    }
	    return;
        } 
    }
}


/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    socketSetOption
 * Signature: (ILjava/lang/Object;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_socketSetOption(JNIEnv *env,
						      jobject this,
						      jint opt,
						      jobject value) {
    int fd;
    int level, optname, optlen = 0;
    union {
        int i;
	char c;
    } optval;

    /*
     * Check that socket hasn't been closed
     */
    fd = getFD(env, this);
    if (fd < 0) { 
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"Socket closed");
	return;
    }

    /*
     * Check argument has been provided
     */
    if (IS_NULL(value)) {
	JNU_ThrowNullPointerException(env, "value argument");
	return;
    }

    /*
     * Setting the multicast interface handled seperately 
     */
    if (opt == java_net_SocketOptions_IP_MULTICAST_IF ||
	opt == java_net_SocketOptions_IP_MULTICAST_IF2) {

	setMulticastInterface(env, this, fd, opt, value);
	return;
    }

    /*
     * Map the Java level socket option to the platform specific
     * level and option name.
     */
    if (NET_MapSocketOption(opt, &level, &optname)) {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Invalid option");
        return;
    }

    switch (opt) {
        case java_net_SocketOptions_SO_SNDBUF :
        case java_net_SocketOptions_SO_RCVBUF :
        case java_net_SocketOptions_IP_TOS :
            {
                jclass cls;
                jfieldID fid;

		cls = (*env)->FindClass(env, "java/lang/Integer");
		CHECK_NULL(cls);
		fid =  (*env)->GetFieldID(env, cls, "value", "I");
		CHECK_NULL(fid);

                optval.i = (*env)->GetIntField(env, value, fid);
                optlen = sizeof(optval.i);
                break;
            }

	case java_net_SocketOptions_SO_REUSEADDR:
	case java_net_SocketOptions_SO_BROADCAST:
	case java_net_SocketOptions_IP_MULTICAST_LOOP:
	    {
		jclass cls;
		jfieldID fid;
		jboolean on;

		cls = (*env)->FindClass(env, "java/lang/Boolean");
		CHECK_NULL(cls);
		fid =  (*env)->GetFieldID(env, cls, "value", "Z");
		CHECK_NULL(fid);

		on = (*env)->GetBooleanField(env, value, fid);
		if (opt == java_net_SocketOptions_IP_MULTICAST_LOOP) {

		    /*
		     * IP_MULTICAST_LOOP may be mapped to IPPROTO (arg
		     * type 'char') or IPPROTO_V6 (arg type 'int').
		     *
		     * In addition setLoopbackMode(true) disables 
		     * IP_MULTICAST_LOOP - doesn't enable it.
		     */
		    if (level == IPPROTO_IP) {
			optval.c = (!on ? 1 : 0);
			optlen = sizeof(optval.c);
		    } else {
			optval.i = (!on ? 1 : 0);
			optlen = sizeof(optval.i);
		    }

		} else {
		    /* SO_REUSEADDR or SO_BROADCAST */
		    optval.i = (on ? 1 : 0);
		    optlen = sizeof(optval.i);
		}

	    	break;
	    }

	default :
	    JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", 
		"Socket option not supported by PlainDatagramSocketImp");
	    break;

    }

    if (NET_SetSockOpt(fd, level, optname, (const void *)&optval, optlen) < 0) {
        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", "Error setting socket option");
	return;
    }

    /*
     * Can't query IPV6_MULTICAST_LOOP on Linux 2.2 kernel so
     * store it in impl so that we can simulate getsockopt.
     */
#if defined(__linux__) && defined(AF_INET6)
    if (isOldKernel && 
	level == IPPROTO_IPV6 && optname == IPV6_MULTICAST_LOOP) {
        (*env)->SetBooleanField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_loopbackID), (optval.c ? JNI_FALSE :JNI_TRUE));
    } 
#endif
}


/*
 * Return the multicast interface:
 *
 * SocketOptions.IP_MULTICAST_IF
 *	IPv4:	Query IPPROTO_IP/IP_MULTICAST_IF
 *		Create InetAddress
 *		IP_MULTICAST_IF returns struct ip_mreqn on 2.2
 *		kernel but struct in_addr on 2.4 kernel
 *	IPv6:	Query IPPROTO_IPV6 / IPV6_MULTICAST_IF or
 *		obtain from impl is Linux 2.2 kernel
 *		If index == 0 return InetAddress representing
 *		anyLocalAddress.
 *		If index > 0 query NetworkInterface by index
 *		and returns addrs[0]
 *
 * SocketOptions.IP_MULTICAST_IF2
 *	IPv4:	Query IPPROTO_IP/IP_MULTICAST_IF
 *		Query NetworkInterface by IP address and
 *		return the NetworkInterface that the address
 *		is bound too.
 *	IPv6:	Query IPPROTO_IPV6 / IPV6_MULTICAST_IF
 *		(except Linux .2 kernel)
 *		Query NetworkInterface by index and
 *		return NetworkInterface.	
 */
jobject getMulticastInterface(JNIEnv *env, jobject this, int fd, jint opt) {
    jboolean isIPV4 = JNI_TRUE;

#ifdef AF_INET6
    if (ipv6_available()) {
	isIPV4 = JNI_FALSE;
    }
#endif

    /*
     * IPv4 implementation
     */
    if (isIPV4) {
	static jclass inet4_class;
	static jmethodID inet4_ctrID;
	static jfieldID inet4_addrID;

	static jclass ni_class;
	static jmethodID ni_ctrID;
	static jfieldID ni_indexID;
	static jfieldID ni_addrsID;

        jobjectArray addrArray;
        jobject addr;
	jobject ni;

        struct in_addr in;
	struct in_addr *inP = &in;
	int len = sizeof(struct in_addr);

	struct ip_mreqn mreqn;
	if (isOldKernel) {
	    inP = (struct in_addr *)&mreqn;
	    len = sizeof(struct ip_mreqn);
	}
        if (JVM_GetSockOpt(fd, IPPROTO_IP, IP_MULTICAST_IF,
                           (char *)inP, &len) < 0) {
            NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                             "Error getting socket option");
            return NULL;
        }

	/*
	 * Construct and populate an Inet4Address
	 */
	if (inet4_class == NULL) {
	    jclass c = (*env)->FindClass(env, "java/net/Inet4Address");
	    CHECK_NULL_RETURN(c, NULL);
	    inet4_ctrID = (*env)->GetMethodID(env, c, "<init>", "()V");
	    CHECK_NULL_RETURN(inet4_ctrID, NULL);
	    inet4_addrID = (*env)->GetFieldID(env, c, "address", "I");
	    CHECK_NULL_RETURN(inet4_addrID, NULL);
	    inet4_class = (*env)->NewGlobalRef(env, c);
	    CHECK_NULL_RETURN(inet4_class, NULL);
	}
	addr = (*env)->NewObject(env, inet4_class, inet4_ctrID, 0);
	CHECK_NULL_RETURN(addr, NULL);

        (*env)->SetIntField(env, addr, inet4_addrID,
		(isOldKernel ? ntohl(mreqn.imr_address.s_addr) : ntohl(in.s_addr)) );

	/*
	 * For IP_MULTICAST_IF return InetAddress
	 */
	if (opt == java_net_SocketOptions_IP_MULTICAST_IF) {
	    return addr;
	}

	/* 
	 * For IP_MULTICAST_IF2 we get the NetworkInterface for
	 * this address and return it
	 */
	if (ni_class == NULL) {
	    jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
	    CHECK_NULL_RETURN(c, NULL);
	    ni_ctrID = (*env)->GetMethodID(env, c, "<init>", "()V");
	    CHECK_NULL_RETURN(ni_ctrID, NULL);
	    ni_indexID = (*env)->GetFieldID(env, c, "index", "I");
	    CHECK_NULL_RETURN(ni_indexID, NULL);
	    ni_addrsID = (*env)->GetFieldID(env, c, "addrs", 
					    "[Ljava/net/InetAddress;");
	    CHECK_NULL_RETURN(ni_addrsID, NULL);
	    ni_class = (*env)->NewGlobalRef(env, c);
	    CHECK_NULL_RETURN(ni_class, NULL);
	}
        ni = Java_java_net_NetworkInterface_getByInetAddress0(env, ni_class, addr);
	if (ni) {
	    return ni;
	}

	/*
	 * The address doesn't appear to be bound at any known
	 * NetworkInterface. Therefore we construct a NetworkInterface
	 * with this address.
	 */
	ni = (*env)->NewObject(env, ni_class, ni_ctrID, 0);
	CHECK_NULL_RETURN(ni, NULL);

	(*env)->SetIntField(env, ni, ni_indexID, -1);
        addrArray = (*env)->NewObjectArray(env, 1, inet4_class, NULL);
	CHECK_NULL_RETURN(addrArray, NULL);
        (*env)->SetObjectArrayElement(env, addrArray, 0, addr);
        (*env)->SetObjectField(env, ni, ni_addrsID, addrArray);
	return ni;
    }


#ifdef AF_INET6
    /*
     * IPv6 implementation
     */
    assert ((opt == java_net_SocketOptions_IP_MULTICAST_IF) ||
            (opt == java_net_SocketOptions_IP_MULTICAST_IF2));
    {
	static jclass ni_class;
	static jmethodID ni_ctrID;
        static jfieldID ni_indexID;
        static jfieldID ni_addrsID;
	static jclass ia_class;
	static jmethodID ia_anyLocalAddressID;

	int index;
	int len = sizeof(index);

	jobjectArray addrArray;
        jobject addr;
	jobject ni; 

	/*
	 * Linux 2.2 kernel doesn't support IPV6_MULTICAST_IF socke option
	 * so use cached index.
	 */
	if (isOldKernel) {
	    index = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_multicastInterfaceID));
	} else
	{
	    if (JVM_GetSockOpt(fd, IPPROTO_IPV6, IPV6_MULTICAST_IF,
			       (char*)&index, &len) < 0) {
		NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                               "Error getting socket option");
		return NULL;
	    }
        }

	if (ni_class == NULL) {
	    jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
	    CHECK_NULL_RETURN(c, NULL);
	    ni_ctrID = (*env)->GetMethodID(env, c, "<init>", "()V");
	    CHECK_NULL_RETURN(ni_ctrID, NULL);
            ni_indexID = (*env)->GetFieldID(env, c, "index", "I");
	    CHECK_NULL_RETURN(ni_indexID, NULL);
            ni_addrsID = (*env)->GetFieldID(env, c, "addrs",
                                            "[Ljava/net/InetAddress;");
	    CHECK_NULL_RETURN(ni_addrsID, NULL);

	    ia_class = (*env)->FindClass(env, "java/net/InetAddress");
	    CHECK_NULL_RETURN(ia_class, NULL);
	    ia_class = (*env)->NewGlobalRef(env, ia_class);
  	    CHECK_NULL_RETURN(ia_class, NULL);
	    ia_anyLocalAddressID = (*env)->GetStaticMethodID(env, 
							     ia_class,
						             "anyLocalAddress",
							     "()Ljava/net/InetAddress;");
	    CHECK_NULL_RETURN(ia_anyLocalAddressID, NULL);
	    ni_class = (*env)->NewGlobalRef(env, c);
	    CHECK_NULL_RETURN(ni_class, NULL);
	}

	/*
	 * If multicast to a specific interface then return the 
	 * interface (for IF2) or the any address on that interface
	 * (for IF).
	 */
	if (index > 0) {
	    ni = Java_java_net_NetworkInterface_getByIndex(env, ni_class,
								   index);
	    if (ni == NULL) {
		char errmsg[255];
		sprintf(errmsg, 
			"IPV6_MULTICAST_IF returned index to unrecognized interface: %d",
			index);
		JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", errmsg);
		return NULL;
	    }

            /*
             * For IP_MULTICAST_IF2 return the NetworkInterface
             */
	    if (opt == java_net_SocketOptions_IP_MULTICAST_IF2) {
                return ni;
            }

	    /*
	     * For IP_MULTICAST_IF return addrs[0] 
	     */
	    addrArray = (*env)->GetObjectField(env, ni, ni_addrsID);
	    if ((*env)->GetArrayLength(env, addrArray) < 1) {
		JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
		    "IPV6_MULTICAST_IF returned interface without IP bindings");
		return NULL;
	    }

	    addr = (*env)->GetObjectArrayElement(env, addrArray, 0);
	    return addr;
  	}

	/* 
	 * Multicast to any address - return anyLocalAddress
	 * or a NetworkInterface with addrs[0] set to anyLocalAddress
	 */

	addr = (*env)->CallStaticObjectMethod(env, ia_class, ia_anyLocalAddressID,
					      NULL);
	if (opt == java_net_SocketOptions_IP_MULTICAST_IF) {
	    return addr;
	}

	ni = (*env)->NewObject(env, ni_class, ni_ctrID, 0);
	CHECK_NULL_RETURN(ni, NULL);
        (*env)->SetIntField(env, ni, ni_indexID, -1);
        addrArray = (*env)->NewObjectArray(env, 1, ia_class, NULL);
	CHECK_NULL_RETURN(addrArray, NULL);
        (*env)->SetObjectArrayElement(env, addrArray, 0, addr);
        (*env)->SetObjectField(env, ni, ni_addrsID, addrArray);
        return ni;
    }
#endif
}



/*
 * Returns relevant info as a jint.
 *
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    socketGetOption
 * Signature: (I)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL
Java_java_net_PlainDatagramSocketImpl_socketGetOption(JNIEnv *env, jobject this,
						      jint opt) {
    int fd;
    int level, optname, optlen;
    union {
        int i;
	char c;
    } optval;

    fd = getFD(env, this);
    if (fd < 0) {
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"socket closed");
	return NULL;
    }

    /*
     * Handle IP_MULTICAST_IF seperately
     */
    if (opt == java_net_SocketOptions_IP_MULTICAST_IF ||
	opt == java_net_SocketOptions_IP_MULTICAST_IF2) {
	return getMulticastInterface(env, this, fd, opt);

    }

    /*
     * SO_BINDADDR implemented using getsockname
     */
    if (opt == java_net_SocketOptions_SO_BINDADDR) {
	/* find out local IP address */
	SOCKADDR him;
	socklen_t len = 0;
	int port;
	jobject iaObj;

	len = SOCKADDR_LEN;

	if (getsockname(fd, (struct sockaddr *)&him, &len) == -1) {
	    NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                                         "Error getting socket name");
	    return NULL;
	}
	iaObj = NET_SockaddrToInetAddress(env, (struct sockaddr *)&him, &port);

	return iaObj;
    }

    /*
     * Map the Java level socket option to the platform specific
     * level and option name.
     */
    if (NET_MapSocketOption(opt, &level, &optname)) {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Invalid option");
        return NULL;
    }

    /*
     * IP_MULTICAST_LOOP socket option isn't available on Linux 2.2 
     * kernel with IPv6 so return value stored in impl.
     */
#if defined(AF_INET6) && defined(__linux__) 
    if (isOldKernel && opt == java_net_SocketOptions_IP_MULTICAST_LOOP &&
	level == IPPROTO_IPV6) {
	int mode = (int)(*env)->GetBooleanField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_loopbackID));
	return createBoolean(env, mode);
    }
#endif

    if (opt == java_net_SocketOptions_IP_MULTICAST_LOOP &&
	level == IPPROTO_IP) {
	optlen = sizeof(optval.c);
    } else {
	optlen = sizeof(optval.i);
    }

    if (NET_GetSockOpt(fd, level, optname, (void *)&optval, &optlen) < 0) {
        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException", 
                         "Error getting socket option");
        return NULL;
    }

    switch (opt) {
	case java_net_SocketOptions_IP_MULTICAST_LOOP:
	    /* getLoopbackMode() returns true if IP_MULTICAST_LOOP disabled */
	    if (level == IPPROTO_IP) {
		return createBoolean(env, (int)!optval.c);
	    } else {
		return createBoolean(env, !optval.i);
	    }

	case java_net_SocketOptions_SO_BROADCAST:
	case java_net_SocketOptions_SO_REUSEADDR:
	    return createBoolean(env, optval.i);

	case java_net_SocketOptions_SO_SNDBUF:
 	case java_net_SocketOptions_SO_RCVBUF:
	case java_net_SocketOptions_IP_TOS:
	    return createInteger(env, optval.i);

    }

    /* should never rearch here */
    return NULL;
}

/*
 * Multicast-related calls
 */

JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_setTTL(JNIEnv *env, jobject this,
					     jbyte ttl) {
    jint ittl = ttl;
    if (ittl < 0) {
        ittl += 0x100;
    }
    Java_java_net_PlainDatagramSocketImpl_setTimeToLive(env, this, (jint)ittl);
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    setTTL
 * Signature: (B)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_setTimeToLive(JNIEnv *env, jobject this,
						    jint ttl) {

    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    int fd;
    /* it is important to cast this to a char, otherwise setsockopt gets confused */

    if (IS_NULL(fdObj)) {
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"Socket closed");
	return;
    } else {
	fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    }
    /* setsockopt to be correct ttl */
#ifdef AF_INET6
    if (ipv6_available()) {
	if (JVM_SetSockOpt(fd, IPPROTO_IPV6, IPV6_MULTICAST_HOPS,
			   (char*)&ttl, sizeof(ttl)) < 0) {
	    NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                           "Error setting socket option");
	    return;
	}
	if (isOldKernel) {
	    (*env)->SetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_ttlID), ttl);
	}

    } else 
#endif /* AF_INET6 */
	{
	    if (JVM_SetSockOpt(fd, IPPROTO_IP, IP_MULTICAST_TTL, (char*)&ttl,
			       sizeof(ttl)) < 0) {
		NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                               "Error setting socket option");
	    }
	}
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    getTTL
 * Signature: ()B
 */
JNIEXPORT jbyte JNICALL
Java_java_net_PlainDatagramSocketImpl_getTTL(JNIEnv *env, jobject this) {
    return (jbyte)Java_java_net_PlainDatagramSocketImpl_getTimeToLive(env, this);
}


/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    getTTL
 * Signature: ()B
 */
JNIEXPORT jint JNICALL
Java_java_net_PlainDatagramSocketImpl_getTimeToLive(JNIEnv *env, jobject this) {

    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint fd = -1;

    if (IS_NULL(fdObj)) {
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"Socket closed");
	return -1;
    } else {
	fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    }
    /* getsockopt of ttl */
#ifdef AF_INET6
    if (ipv6_available()) {
	int ttl = 0;
	int len = sizeof(ttl);

	/*
	 * Linux 2.2 kernel doesn't support IPV6_MULTICAST_HOPS socket option
	 */
	if (isOldKernel) {
	    return (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_ttlID));
	}

	if (JVM_GetSockOpt(fd, IPPROTO_IPV6, IPV6_MULTICAST_HOPS,
			       (char*)&ttl, &len) < 0) {
		NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                               "Error getting socket option");
		return -1;
	    }
	return (jint)ttl;
    } else 
#endif /* AF_INET6 */
	{
	    jint ttl = 0;
	    int len = sizeof(ttl);
	    if (JVM_GetSockOpt(fd, IPPROTO_IP, IP_MULTICAST_TTL,
			       (char*)&ttl, &len) < 0) {
		NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "SocketException",
                               "Error getting socket option");
		return -1;
	    }
	    return ttl;
	}
}

/*
 * mcast_join_leave: Join or leave a multicast group.
 *
 * For IPv4 sockets use IP_ADD_MEMBERSHIP/IP_DROP_MEMBERSHIP socket option
 * to join/leave multicast group.
 *
 * For IPv6 sockets use IPV6_ADD_MEMBERSHIP/IPV6_DROP_MEMBERSHIP socket option
 * to join/leave multicast group. If multicast group is an IPv4 address then
 * an IPv4-mapped address is used.
 *
 * On Linux with IPv6 if we wish to join/leave an IPv4 multicast group then
 * we must use the IPv4 socket options. This is because the IPv6 socket options
 * don't support IPv4-mapped addresses. This is true as per 2.2.19 and 2.4.7
 * kernel releases. In the future it's possible that IP_ADD_MEMBERSHIP 
 * will be updated to return ENOPROTOOPT if uses with an IPv6 socket (Solaris
 * already does this). Thus to cater for this we first try with the IPv4
 * socket options and if they fail we use the IPv6 socket options. This
 * seems a reasonable failsafe solution.
 */
static void mcast_join_leave(JNIEnv *env, jobject this,
			     jobject iaObj, jobject niObj,
			     jboolean join) {

    jobject fdObj = (*env)->GetObjectField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_fdID));
    jint fd;    
    jint ipv6_join_leave;

    if (IS_NULL(fdObj)) {
	JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
		        "Socket closed");
        return;
    } else {
        fd = (*env)->GetIntField(env, fdObj, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, IO_fd_fdID));
    }
    if (IS_NULL(iaObj)) {
        JNU_ThrowNullPointerException(env, "iaObj");
        return;
    }

    /*
     * Determine if this is an IPv4 or IPv6 join/leave.
     */
#ifdef AF_INET6 
    ipv6_join_leave = ipv6_available();

#ifdef __linux__
    if ((*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_familyID)) == IPv4) {
	ipv6_join_leave = JNI_FALSE;
    }
#endif

#else
    /*
     * IPv6 not compiled in
     */
    ipv6_join_leave = JNI_FALSE;
#endif

    /*
     * For IPv4 join use IP_ADD_MEMBERSHIP/IP_DROP_MEMBERSHIP socket option
     *
     * On Linux if IPv4 or IPv6 use IP_ADD_MEMBERSHIP/IP_DROP_MEMBERSHIP
     */
    if (!ipv6_join_leave) {
#ifdef __linux__
	struct ip_mreqn mname;
#else
	struct ip_mreq mname;
#endif
	int mname_len = 0;

	/*
   	 * joinGroup(InetAddress, NetworkInterface) implementation :-
	 *
	 * Linux/IPv6:	use ip_mreqn structure populated with multicast
 	 *		address and interface index.
	 *
	 * IPv4:	use ip_mreq structure populated with multicast
	 *		address and first address obtained from
	 *		NetworkInterface
	 */
	if (niObj != NULL) {
#if defined(__linux__) && defined(AF_INET6) 
	    if (ipv6_available()) {
		static jfieldID ni_indexID;

                if (ni_indexID == NULL) {
                    jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
		    CHECK_NULL(c);
                    ni_indexID = (*env)->GetFieldID(env, c, "index", "I");
		    CHECK_NULL(ni_indexID);
                }

		mname.imr_multiaddr.s_addr = htonl((*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID)));
		mname.imr_address.s_addr = 0;
		mname.imr_ifindex =  (*env)->GetIntField(env, niObj, ni_indexID);
		mname_len = sizeof(struct ip_mreqn);
	    } else 
#endif
	    {
	        static jfieldID ni_addrsID;
		jobjectArray addrArray;
		jobject addr;

	        if (ni_addrsID == NULL) {
		    jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
		    CHECK_NULL(c);
		    ni_addrsID = (*env)->GetFieldID(env, c, "addrs", 
						    "[Ljava/net/InetAddress;");
		    CHECK_NULL(ni_addrsID);
	        }
	    
	        addrArray = (*env)->GetObjectField(env, niObj, ni_addrsID);
		if ((*env)->GetArrayLength(env, addrArray) < 1) {
		    JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
                    	"bad argument for IP_ADD_MEMBERSHIP: "
			"No IP addresses bound to interface");
		    return;
		}
		addr = (*env)->GetObjectArrayElement(env, addrArray, 0);

		mname.imr_multiaddr.s_addr = htonl((*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID)));
#ifdef __linux__
                mname.imr_address.s_addr = htonl((*env)->GetIntField(env, addr, JNI_STATIC(java_net_InetAddress, ia_addressID)));
#else
		mname.imr_interface.s_addr = htonl((*env)->GetIntField(env, addr, JNI_STATIC(java_net_InetAddress, ia_addressID)));
#endif
		mname_len = sizeof(struct ip_mreq);
	    }
	}


	/*
         * joinGroup(InetAddress) implementation :-
         *
         * Linux/IPv6:  use ip_mreqn structure populated with multicast
         *              address and interface index. index obtained
	 *		from cached value or IPV6_MULTICAST_IF.
         *
         * IPv4:        use ip_mreq structure populated with multicast
         *              address and local address obtained from
         *              IP_MULTICAST_IF. On Linux IP_MULTICAST_IF
	 *		returns different structure depending on 
	 *		kernel.
         */

	if (niObj == NULL) {

#if defined(__linux__) && defined(AF_INET6)
	    if (ipv6_available()) {

            	int index;
            	int len = sizeof(index);

            	if (isOldKernel) {
                    index = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_multicastInterfaceID));
                } else {
                    if (JVM_GetSockOpt(fd, IPPROTO_IPV6, IPV6_MULTICAST_IF,
                                       (char*)&index, &len) < 0) {
                        NET_ThrowCurrent(env, "getsockopt IPV6_MULTICAST_IF failed");
                        return;
		    }
                }

		mname.imr_multiaddr.s_addr = htonl((*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID)));
		mname.imr_address.s_addr = 0 ;
                mname.imr_ifindex = index;
		mname_len = sizeof(struct ip_mreqn);
	    } else 
#endif
	    {
	        struct in_addr in;
                struct in_addr *inP = &in;
                socklen_t len = sizeof(struct in_addr);

#ifdef __linux__
                struct ip_mreqn mreqn;
                if (isOldKernel) {
                    inP = (struct in_addr *)&mreqn;
                    len = sizeof(struct ip_mreqn);
		}
#endif
	        if (getsockopt(fd, IPPROTO_IP, IP_MULTICAST_IF, (char *)inP, &len) < 0) {
		    NET_ThrowCurrent(env, "getsockopt IP_MULTICAST_IF failed");
		    return;
	        }

#ifdef __linux__
	        mname.imr_address.s_addr =
                    (isOldKernel ? mreqn.imr_address.s_addr : in.s_addr);

#else
	        mname.imr_interface.s_addr = in.s_addr;
#endif
		mname.imr_multiaddr.s_addr = htonl((*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID)));
		mname_len = sizeof(struct ip_mreq);
	    }
	}


	/* 
	 * Join the multicast group.
	 */
	if (JVM_SetSockOpt(fd, IPPROTO_IP, (join ? IP_ADD_MEMBERSHIP:IP_DROP_MEMBERSHIP),
		           (char *) &mname, mname_len) < 0) {

	    /*
	     * If IP_ADD_MEMBERSHIP returns ENOPROTOOPT on Linux and we've got
	     * IPv6 enabled then it's possible that the kernel has been fixed
	     * so we switch to IPV6_ADD_MEMBERSHIP socket option.
	     * As of 2.4.7 kernel IPV6_ADD_MEMERSHIP can't handle IPv4-mapped
	     * addresses so we have to use IP_ADD_MEMERSHIP for IPv4 multicast
	     * groups. However if the socket is an IPv6 socket then then setsockopt
	     * should reurn ENOPROTOOPT. We assume this will be fixed in Linux
	     * at some stage.
	     */
#if defined(__linux__) && defined(AF_INET6)
	    if (errno == ENOPROTOOPT) {
		if (ipv6_available()) {
		    ipv6_join_leave = JNI_TRUE;
		    errno = 0;
		} else  {
		    errno = ENOPROTOOPT;    /* errno can be changed by ipv6_available */
		}
	    } 
#endif	    	    
	    if (errno) {
		if (join) {
		    NET_ThrowCurrent(env, "setsockopt IP_ADD_MEMBERSHIP failed");
		} else {
		    if (errno == ENOENT) 
			JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			    "Not a member of the multicast group");
		    else 
		        NET_ThrowCurrent(env, "setsockopt IP_DROP_MEMBERSHIP failed");
		}
	    }
	}

	/*
	 * If we haven't switched to IPv6 socket option then we're done.
	 */
	if (!ipv6_join_leave) {
	    return;
	}
    }


    /* 
     * IPv6 join. If it's an IPv4 multicast group then we use an IPv4-mapped
     * address.
     */
#ifdef AF_INET6
    {
	struct ipv6_mreq mname6;
	jbyteArray ipaddress;
	jbyte caddr[16];
	jint family;
	jint address;
	family = (*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_familyID)) == IPv4? AF_INET : AF_INET6;
	if (family == AF_INET) { /* will convert to IPv4-mapped address */
	    memset((char *) caddr, 0, 16);
	    address = (*env)->GetIntField(env, iaObj, JNI_STATIC(java_net_InetAddress, ia_addressID));

	    caddr[10] = 0xff;
	    caddr[11] = 0xff;

	    caddr[12] = ((address >> 24) & 0xff);
	    caddr[13] = ((address >> 16) & 0xff);
	    caddr[14] = ((address >> 8) & 0xff);
	    caddr[15] = (address & 0xff); 
	} else {
	    jfieldID ia_ipaddress;
	    jclass c = (*env)->FindClass(env, "java/net/Inet6Address");
	    CHECK_NULL(c);
	    ia_ipaddress = (*env)->GetFieldID(env, c, "ipaddress", "[B");
	    ipaddress = (*env)->GetObjectField(env, iaObj, ia_ipaddress);
	    
	    (*env)->GetByteArrayRegion(env, ipaddress, 0, 16, caddr);
	}

	memcpy((void *)&(mname6.ipv6mr_multiaddr), caddr, sizeof(struct in6_addr));
	if (IS_NULL(niObj)) {
	    int index;
	    int len = sizeof(index);

#ifdef __linux__
	    /*
	     * 2.2 kernel doens't support IPV6_MULTICAST_IF socket option
	     */
	    if (isOldKernel) {
		index = (*env)->GetIntField(env, this, JNI_STATIC_MD(java_net_PlainDatagramSocketImpl, pdsi_multicastInterfaceID));
	    } else 
#endif
	    {
		if (JVM_GetSockOpt(fd, IPPROTO_IPV6, IPV6_MULTICAST_IF,
				 (char*)&index, &len) < 0) {
		    NET_ThrowCurrent(env, "getsockopt IPV6_MULTICAST_IF failed");
		    return;
		}
	    }

#ifdef __linux__
	    /*
	     * On 2.4.8+ if we join a group with the interface set to 0
	     * then the kernel records the interface it decides. This causes
	     * subsequent leave groups to fail as there is no match. Thus we
	     * pick the interface if there is a matching route. 
	     */
	    if (index == 0 && !isOldKernel) {
		int rt_index = getDefaultIPv6Interface(&(mname6.ipv6mr_multiaddr));
		if (rt_index > 0) {
		    index = rt_index;
		}
	    }
#endif

	    mname6.ipv6mr_interface = index;
	} else {
	    static jfieldID ni_indexID;

	    if (ni_indexID == NULL) {
		jclass c = (*env)->FindClass(env, "java/net/NetworkInterface");
		CHECK_NULL(c);
		ni_indexID = (*env)->GetFieldID(env, c, "index", "I");
		CHECK_NULL(ni_indexID);
	    }
	
	    mname6.ipv6mr_interface =
		(*env)->GetIntField(env, niObj, ni_indexID);
	}

	/* Join the multicast group */
	if (JVM_SetSockOpt(fd, IPPROTO_IPV6, (join ? IPV6_ADD_MEMBERSHIP : IPV6_DROP_MEMBERSHIP), 
			   (char *) &mname6, sizeof (mname6)) < 0) {

	    if (join) {
		NET_ThrowCurrent(env, "setsockopt IPV6_ADD_MEMBERSHIP failed");
	    } else {
		if (errno == ENOENT) {
		   JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException",
			"Not a member of the multicast group");
		} else {
		    NET_ThrowCurrent(env, "setsockopt IPV6_DROP_MEMBERSHIP failed");
		}
	    }
	}
    }
#endif
}

/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    join
 * Signature: (Ljava/net/InetAddress;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_join(JNIEnv *env, jobject this,
					   jobject iaObj, jobject niObj) {
    mcast_join_leave(env, this, iaObj, niObj, JNI_TRUE);
}


/*
 * Class:     java_net_PlainDatagramSocketImpl
 * Method:    leave
 * Signature: (Ljava/net/InetAddress;)V
 */
JNIEXPORT void JNICALL
Java_java_net_PlainDatagramSocketImpl_leave(JNIEnv *env, jobject this,
					    jobject iaObj, jobject niObj) {
    mcast_join_leave(env, this, iaObj, niObj, JNI_FALSE);
}
