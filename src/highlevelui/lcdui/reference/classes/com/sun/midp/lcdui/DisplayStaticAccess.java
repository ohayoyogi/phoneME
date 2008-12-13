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

package com.sun.midp.lcdui;

import javax.microedition.lcdui.Display;


/**
 * This is the interface to "tunnel" across Java package namespace,
 * and call the protected methods in the another package namespace.
 * The callee package will implement this interface, and provide
 * a static utility instance to the caller package.
 */
public interface DisplayStaticAccess {
    /**
     * Gets the <code>Display</code> object by owner, create one if needed.
     *
     * @param owner the owner of the display, the owner can be any class
     *
     * @return the display object that application can use for its user
     * interface
     *
     * @throws NullPointerException if <code>owner</code> is <code>null</code>
     */
    Display getDisplay(Object owner);

    /**
     * Free a <code>Display</code> no longer in use.
     *
     * @param owner the owner of the display, the owner can be any class
     *
     * @return true if display has been succcessfully removed, 
     *         false, if display object has not been found.
     *
     * @throws NullPointerException if <code>owner</code> is <code>null</code>
     */
    public boolean freeDisplay(Object owner);
} 
