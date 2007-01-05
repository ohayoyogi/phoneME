/*
 *
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
 */

package com.sun.midp.main;

import com.sun.midp.events.EventTypes;
import com.sun.midp.events.NativeEvent;
import com.sun.midp.events.EventQueue;

import com.sun.midp.security.Permissions;
import com.sun.midp.security.SecurityToken;

/**
 * This class provides methods to send events of types
 * handled by MIDletExecuteEventConsumer I/F implementors.
 * This class completely hide event construction & sending in its methods.
 *
 * The only user of this class in AppIsolateMIDletSuiteLoader in
 * application isolate (when no midlets & no midlet suites exist).
 *
 * Generic comments for all XXXEventProducers:
 *
 * For each supported event type there is a separate sendXXXEvent() method,
 * that gets all needed parameters to construct an event of an approprate class.
 * The method also performs event sending itself.
 *
 * If a given event type merges a set of logically different subtypes,
 * this class shall provide separate methods for these subtypes.
 *
 * It is assumed that only one object instance of this class (per isolate)
 * is created at (isolate) startup.
 * All MIDP stack subsystems that need to send events of supported types,
 * must get a reference to an already created istance of this class.
 * Typically, this instance should be passed as a constructor parameter.
 *
 * For security reasons constructor is not public.
 * Use createXXXProducer(...) method,
 * protected by security, to create and object instance of this class
 * from a different package.
 *
 * Class is NOT final to allow debug/profile/test/automation subsystems
 * to change, substitute, complement default "event sending" functionality :
 * Ex.
 * class LogXXXEventProducer
 *      extends XXXEventProducer {
 *  ...
 *  void sendXXXEvent(parameters) {
 *      LOG("Event of type XXX is about to be sent ...")
 *      super.sendXXXEvent(parameters);
 *      LOG("Event of type XXX has been sent successfully !")
 *  }
 *  ...
 * }
 */
public class MIDletExecuteEventProducer {

    /** Cached reference to the MIDP event queue. */
    protected EventQueue eventQueue;
    /** Cached reference to AMS isolate ID. */
    protected int amsIsolateId;

    /**
     * Construct a new MIDletExecuteEventProducer.
     *
     * @param  token security token that controls instance creation.
     * @param  theEventQueue An event queue where new events will be posted.
     * @param  theAmsIsolateId AMS Isolate Id
     */
    public MIDletExecuteEventProducer(
        SecurityToken token,
        EventQueue theEventQueue,
        int theAmsIsolateId) {

        token.checkIfPermissionAllowed(Permissions.MIDP);
        eventQueue = theEventQueue;
        amsIsolateId = theAmsIsolateId;
    }

    /*
     * MIDlet Execute Events:
     *
     * EXECUTE_MIDLET
     */
    /**
     * Called to request MIDlet execution from non-AMS isolate
     * NEW: earlier generated by AMSUtil.executeWithArgs(...)
     *
     * @param midletExternalAppId ID of MIDlet to invoke, given by an external
     *                      application manager
     * @param midletSuiteId ID of an installed suite
     * @param midletClassName class name of MIDlet to invoke
     * @param midletDisplayName name to display to the user
     * @param arg0 if not null, this parameter will be available to the
     *             MIDlet as application property arg-0
     * @param arg1 if not null, this parameter will be available to the
     *             MIDlet as application property arg-1
     * @param arg2 if not null, this parameter will be available to the
     *             MIDlet as application property arg-2
     * @param memoryReserved the minimum amount of memory guaranteed to be
     *             available to the isolate at any time; &lt; 0 if not used
     * @param memoryTotal the total amount of memory that the isolate can
                   reserve; &lt; 0 if not used
     * @param priority priority to set for the new isolate;
     *                 &lt;= 0 if not used
     * @param profileName name of the profile to set for the new isolate;
     *                    null if not used
     */
    public void sendMIDletExecuteEvent(
        int midletExternalAppId, int midletSuiteId,
        String midletClassName, String midletDisplayName,
        String arg0, String arg1, String arg2,
        int memoryReserved, int memoryTotal, int priority,
        String profileName) {

        NativeEvent event =
            new NativeEvent(EventTypes.EXECUTE_MIDLET_EVENT);

        event.intParam1 = midletExternalAppId;
        event.intParam2 = midletSuiteId;
        event.intParam3 = memoryReserved;
        event.intParam4 = memoryTotal;
        event.intParam5 = priority;
        event.stringParam1 = midletClassName;
        event.stringParam2 = midletDisplayName;
        event.stringParam3 = arg0;
        event.stringParam4 = arg1;
        event.stringParam5 = arg2;
        event.stringParam6 = profileName;

        eventQueue.sendNativeEventToIsolate(event, amsIsolateId);
    };
}
