/*
 * Copyright 1990-2006 Sun Microsystems, Inc. All Rights Reserved. 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 only,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included at /legal/license.txt).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 or visit www.sun.com if you need additional information or have
 * any questions.
 */
#ifndef _QtEVENT_H_
#define _QtEVENT_H_

#include <qevent.h>

class QtEvent : public QCustomEvent {
public :
    enum Type {
        DeferredDelete = (int)(QEvent::User+1),
        MethodCall,
        Key,
        // 6253974 - the robot needs to be able to send either a MouseButton or
        // a MouseMove event - so have two types here instead of one generic
        // Mouse type
        MouseMove,
        MouseButton
    };
    QtEvent(Type type, 
            void *data=NULL) : QCustomEvent((QEvent::Type)type,data){}
};

/**
 * <Code>QtEventObject</code> encapsulates the Qt event source along with
 * the QEvent object
 */
typedef struct {
    QEvent  *event;
    QObject *source;
} QtEventObject;
    
#endif /* _QtEVENT_H_ */
