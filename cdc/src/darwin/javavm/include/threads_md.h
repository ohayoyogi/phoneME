/*
 * @(#)threads_md.h	1.32 06/10/27
 *
 * Copyright  1990-2006 Sun Microsystems, Inc. All Rights Reserved.  
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
 *
 */

/*
 * Machine-dependent thread definitions.
 */

#ifndef _LINUX_THREADS_MD_H
#define _LINUX_THREADS_MD_H

#include "portlibs/posix/threads.h"
#include "javavm/include/porting/sync.h"
#include "javavm/include/threads_arch.h"
#include "javavm/include/utils.h"

#include <pthread.h>
#include <semaphore.h>
#include <setjmp.h>

#if 0
/*
 * TODO: pthread_kill() and pthread_sigmask() are not supported on darwin.
 * However, they probably aren't necessary. The main use is in sync_md.c
 * to make sure pending io calls on a closed fd are cancelled. This was
 * originally done for linux, but doesn't appear to be necessary for darwin.
 * The other use is by CVMthreadSuspend, which id only used by jvmti and
 * jvmpi, which aren't really that important. In any case, starting with
 * MacOS 10.2, pthread_kill is supported, so we could conditionally add
 * support if necessary.
 */
#include "javavm/include/porting/system.h"
#define pthread_kill(x,y) CVMconsolePrintf("pthread_kill not supported!")
#define pthread_sigmask(x,y,z) CVMconsolePrintf("pthread_sigmask not supported!")
#endif

struct CVMThreadID {
    pthread_t pthreadCookie;
    void *stackTop;
    pthread_mutex_t lock;
    /* support for CVMmutexSetOwner */
    POSIXMutex locked;
    /* support for CVMthreadSuspend */
    int suspended;
    int suspended_in_wait;
    int in_sigsuspend;
    pthread_cond_t suspend_cv;
    /* support for Thread.interrupt */
    CVMBool interrupted;
    CVMBool in_wait;
    CVMBool notified;
    /* emulate a semaphore */
    pthread_mutex_t wait_mutex;
    pthread_cond_t wait_cv;
    int value;
    /* support for close */
    int fd;	/* only valid while thread is on I/O queue! */

    CVMThreadArchData  archData;

    /* IO or wait queue */
    CVMThreadID *next;
    CVMThreadID **prev_p;
};

#define POSIX_COOKIE(t)		((t)->pthreadCookie)

#define CVMthreadCreate		POSIXthreadCreate
#define CVMthreadSelf		POSIXthreadGetSelf

#define CVMthreadGetPriority	POSIXthreadGetPriority
#define CVMthreadSetPriority	POSIXthreadSetPriority


/* NOTE: The current redzone and stack sizes below are taken straight from the
         the debug version of Solaris port.  These numbers will probably fine 
	 for the Linux port but may not be optimum.  To obtain the optimum numbers, 
	 one will have to run a static stack analysis written for analyzing the
         assembly codes of CVM on the Linux platform. 
*/
#define CVM_THREAD_MIN_C_STACK_SIZE				(32 * 1024)

/*
 * The static stack analysis tool doesn't know additional stack requirement
 * for the execution of library functions.
 * Use a reasonable value for the library function overhead.
 */
#define CVM_REDZONE_Lib_Overhead                                 (3 * 1024)

#define CVM_REDZONE_ILOOP                                       \
    (5976 + CVM_REDZONE_Lib_Overhead) /*  8.84KB */
#define CVM_REDZONE_CVMimplementsInterface                      \
    (1680 + CVM_REDZONE_Lib_Overhead) /*  4.64KB */
#define CVM_REDZONE_CVMCstackCVMpc2string                       \
    (1680 + CVM_REDZONE_Lib_Overhead) /*  4.64KB */
#define CVM_REDZONE_CVMCstackCVMID_objectGetClassAndHashSafe    \
    (1944 + CVM_REDZONE_Lib_Overhead) /*  4.90KB */
#define CVM_REDZONE_CVMclassLookupFromClassLoader               \
    (6704 + CVM_REDZONE_Lib_Overhead) /*  9.55KB */
#define CVM_REDZONE_CVMclassLink                                \
    (3592 + CVM_REDZONE_Lib_Overhead) /*  6.51KB */
#define CVM_REDZONE_CVMCstackmerge_fullinfo_to_types            \
    (1936 + CVM_REDZONE_Lib_Overhead) /*  4.89KB */
#define CVM_REDZONE_CVMsignalErrorVaList                        \
    (6040 + CVM_REDZONE_Lib_Overhead) /* 8.90KB */
#define CVM_REDZONE_CVMJITirdumpIRNode                          \
    (1024 + CVM_REDZONE_Lib_Overhead) /* 4KB */
#define CVM_REDZONE_disassembleRangeWithIndentation             \
    (1024 + CVM_REDZONE_Lib_Overhead) /* 4KB */
#define CVM_REDZONE_CVMcpFindFieldInSuperInterfaces             \
    (1024 + CVM_REDZONE_Lib_Overhead)

#endif /* _LINUX_THREADS_MD_H */
