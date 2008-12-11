/*
 *   
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

#ifndef _JSR120_CBS_PROTOCOL_H_
#define _JSR120_CBS_PROTOCOL_H_

/**
 * @file
 * @ingroup stack
 */

/**
 * @defgroup cbs Cell Broadcast Service (CBS)
 * @ingroup wma
 */

/**
 * @file
 * @defgroup cbsprotocol CBS Protocol Porting Interface
 * @ingroup cbs
 * @brief Cell Broadcast Service protocol porting interface. \n
 * ##include <wma_cbs_pool.h>
 * @{
 *
 * This file defines the Cell Broadcast Service protocol porting interfaces.
 * CBS communications operations such as open, close, read and write can be
 * accomplished through these interfaces. Every platform MUST have its own
 * implementation for these interfaces.
 */

#ifdef __cplusplus
extern "C" {
#endif

#include <jsr120_types.h>
#include <jsr120_cbs_pool.h>

/**
 * Register a CBS Message Identifier.
 * <p>
 * For a CBS message, a message identifier is used as the source/type. MIDP WMA
 * applications use this API to subscribe to a specified CBS message. In this format,
 * a message identifier is analogous to a port number. After subscribing to a specified
 * CBS message, WMA will continue to listen for the CBS messages that match the registered
 * message identifier.
 *
 * @param msgID The subscribed CBS message identifier.
 *
 * @return Returns enum WMA_STATUS, WMA_OK on success or
 *	   WMA_ERR on error.
 *
 */
WMA_STATUS jsr120_add_cbs_listening_msgID(jchar msgID);

/**
 * Clear a registered CBS Message Identifier.
 * <p>
 * Unsubscribe a specified CBS message. After calling this API, a WMA application
 * will no longer receive CBS messages whose messsage identifier is specified by
 * <code>msgID</code>.
 *
 * @param msgID The unsubscribed CBS message identifier.
 *
 * @return Returns enum WMA_STATUS, WMA_OK on success or
 *	   WMA_ERR on error.
 *
 */
WMA_STATUS jsr120_remove_cbs_listening_msgID(jchar msgID);

/**
 * Incoming CBS Message.
 * <p>
 * After a WMA application registers a specified message identifier, it will continue to
 * listen for incoming CBS messages whose message ID is equal to the registered message ID.
 * This call back function will be called by native CBS to notify WMA about the incoming
 * WMA message.
 *
 * @param msgType	The encoding type of the incoming message
 *			(GSM 7-bit alphabet, Unicode or 8-bit Binary)
 * @param msgID         Message ID
 * @param msgBuffer	The incoming message body.
 * @param msgLen	The length of the incoming message body.
 */
void jsr120_notify_incoming_cbs(jchar msgType, jchar msgID,
                                unsigned char *msgBuffer, jint msgLen);

/**:
 * The number of protocol segments required to send the given message is computed and
 * returned by this function.
 *
 * @param msgBuffer the message body
 * @param msgLen message body length (in bytes)
 * @param msgType the message type:  Binary or Text
 * @param hasPort indicates if the message includes source or destination port number.
 * @param numSegments The number of message segments is returned here.
 *
 * @return Returns enum WMA_STATUS, WMA_OK on success or
 *	   WMA_ERR on error.
 */
WMA_STATUS jsr120_number_of_cbs_segments(unsigned char msgBuffer[], jint msgLen, jint msgType, 
                                            jboolean hasPort, /*out*/jint *numSegments);


#ifdef __cplusplus
}
#endif

/** @} */ //End of group cbs

#endif /* _JSR120_CBS_PROTOCOL_H_ */
