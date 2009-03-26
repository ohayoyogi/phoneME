/*
 * @(#)PTimerScheduleFailedException.java	1.9 06/10/10
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
 *
 */

package com.sun.util;

/**
 * An exception thrown by the schedule() method of PTimer when a timer 
 * specification can not be met.
 */
public class PTimerScheduleFailedException extends java.lang.Exception {
    /**
     * Constructs a PTimerScheduleFailedException with null as its error 
     * detail message.
     */
    public PTimerScheduleFailedException() {
        super();
    }

    /**
     * Constructs a PTimerScheduleFailedException with the specified 
     * detail message.
     * The error message string s can later be retreived by Trowable.getMessage() 
     * method of java.lang.Throwable.
     *
     * @param s the detail message.
     */
    public PTimerScheduleFailedException(String s) {
        super(s);
    }
}
