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

/**
 * This interface should be implemented by a client that needs a popup keyboard.
 */


public interface VirtualKeyboardListener {

    /**
     * key press callback
     * @param keycode char selected by the user from the virtual keyboard
     */
    public void virtualKeyPressed(int keycode);

    /**
     * key release callback
     * @param keycode char selected by the user from the virtual keyboard
     */
    public void virtualKeyReleased(int keycode);
    /**
     * key repeated callback
     * @param keycode char selected by the user from the virtual keyboard
     */
    public void virtualKeyRepeated(int keycode);

    /**
     * should trigger a requestRepaint() call to schedule a
     * paint() of the VirtualKeyboard
     */
    public void repaintVirtualKeyboard();


}
