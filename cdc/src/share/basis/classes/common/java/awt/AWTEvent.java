/*
 * @(#)AWTEvent.java	1.11 03/11/25
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
 * Warning :
 * Two versions of this file exist in this workspace.
 * One for Personal Basis, and one for Personal Profile.
 * Don't edit the wrong one !!!
 */
package java.awt;

import java.util.EventObject;
import java.awt.event.*;

/**
 * The root event class for all AWT events.
 * This class and its subclasses supercede the original
 * java.awt.Event class.
 * Subclasses of this root AWTEvent class defined outside of the
 * java.awt.event package should define event ID values greater than
 * the value defined by RESERVED_ID_MAX.
 *
 * The event masks defined in this class are needed ONLY by
 * component subclasses which are using Component.enableEvents()
 * to select for event types not selected by registered listeners.
 * If a listener is registered on a component, the appropriate event
 * mask is already set internally by the component.
 * @see Component#enableEvents
 *
 * @see java.awt.event.ComponentEvent
 * @see java.awt.event.FocusEvent
 * @see java.awt.event.KeyEvent
 * @see java.awt.event.MouseEvent
 * @see java.awt.event.WindowEvent
 * @see java.awt.event.ActionEvent
 * @see java.awt.event.AdjustmentEvent
 * @see java.awt.event.ItemEvent
 * @see java.awt.event.TextEvent
 *
 * @version 1.22 07/01/98
 * @author Carl Quinn
 * @author Amy Fowler
 */
public abstract class AWTEvent extends EventObject {
    private transient int data;
    protected int id;
    // This field controls whether or not the event is sent back
    // down to the peer once the source has processed it -
    // false means it's sent to the peer, true means it's not.
    // Semantic events always have a 'true' value since they were
    // generated by the peer in response to a low-level event.
    protected boolean consumed = false;

    transient boolean focusManagerIsDispatching = false;
    /**
     * The event mask for selecting component events.
     */
    public final static long COMPONENT_EVENT_MASK = 0x01;
    /**
     * The event mask for selecting container events.
     */
    public final static long CONTAINER_EVENT_MASK = 0x02;
    /**
     * The event mask for selecting focus events.
     */
    public final static long FOCUS_EVENT_MASK = 0x04;
    /**
     * The event mask for selecting key events.
     */
    public final static long KEY_EVENT_MASK = 0x08;
    /**
     * The event mask for selecting mouse events.
     */
    public final static long MOUSE_EVENT_MASK = 0x10;
    /**
     * The event mask for selecting mouse motion events.
     */
    public final static long MOUSE_MOTION_EVENT_MASK = 0x20;
    /**
     * The event mask for selecting window events.
     */
    public final static long WINDOW_EVENT_MASK = 0x40;
    /**
     * The event mask for selecting action events.
     */
    public final static long ACTION_EVENT_MASK = 0x80;
    /**
     * The event mask for selecting adjustment events.
     */
    public final static long ADJUSTMENT_EVENT_MASK = 0x100;
    /**
     * The event mask for selecting item events.
     */
    public final static long ITEM_EVENT_MASK = 0x200;
    /**
     * The event mask for selecting text events.
     */
    public final static long TEXT_EVENT_MASK = 0x400;
    /**
     * The event mask for selecting input method events.
     */
    public final static long INPUT_METHOD_EVENT_MASK = 0x800;
	
    /**
     * The pseudo event mask for enabling input methods.
     * We're using one bit in the eventMask so we don't need
     * a separate field inputMethodsEnabled.
     */
    final static long INPUT_METHODS_ENABLED_MASK = 0x1000;

    /**
     * The event mask for selecting paint events.
     */
    public final static long PAINT_EVENT_MASK = 0x2000;

    /**
     * The event mask for selecting invocation events.
     */
    public final static long INVOCATION_EVENT_MASK = 0x4000;

    /**
     * The event mask for selecting mouse wheel events.
     * @since 1.4
     */
    public final static long MOUSE_WHEEL_EVENT_MASK = 0x20000;

    /**
     * The event mask for selecting window state events.
     * @since 1.4
     */
//    public final static long WINDOW_STATE_EVENT_MASK = 0x40000;

    /**
     * The event mask for selecting window focus events.
     */
     public final static long WINDOW_FOCUS_EVENT_MASK = 0x80000;

    /**
     * The maximum value for reserved AWT event IDs. Programs defining
     * their own event IDs should use IDs greater than this value.
     */
    public final static int RESERVED_ID_MAX = 1999;
    /*
     * JDK 1.1 serialVersionUID
     */
    private static final long serialVersionUID = -1825314779160409405L;
    /**
     * Constructs an AWTEvent object with the specified source object and type.
     * @param source the object where the event originated
     * @id the event type
     */
    public AWTEvent(Object source, int id) {
        super(source);
        this.id = id;
        switch (id) {
        case ActionEvent.ACTION_PERFORMED:
        case ItemEvent.ITEM_STATE_CHANGED:
        case AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED:
        case TextEvent.TEXT_VALUE_CHANGED:
            consumed = true;
            break;

        default:
        }
    }

    /**
     * Returns the event type.
     */
    public int getID() {
        return id;
    }

    public String toString() {
        String srcName = null;
        if (source instanceof Component) {
            srcName = ((Component) source).getName();
        }
        /*
         else if (source instanceof MenuComponent) {
         srcName = ((MenuComponent)source).getName();
         }
         */
        return getClass().getName() + "[" + paramString() + "] on " +
            (srcName != null ? srcName : source);
    }

    public String paramString() {
        return "";
    }

    protected void consume() {
        switch (id) {
        case KeyEvent.KEY_PRESSED:
        case KeyEvent.KEY_RELEASED:
        case MouseEvent.MOUSE_PRESSED:
        case MouseEvent.MOUSE_RELEASED:
        case MouseEvent.MOUSE_MOVED:
        case MouseEvent.MOUSE_DRAGGED:
        case MouseEvent.MOUSE_ENTERED:
        case MouseEvent.MOUSE_EXITED:
            consumed = true;
            break;

        default:
            // event type cannot be consumed
        }
    }

    protected boolean isConsumed() {
        return consumed;
    }

    /* Package-private method to change a KeyEvent's source to the new
     * focus owner.  This method needs to be here instead of in KeyEvent
     * because it should only be called from by the EventQueue.
     */
    // 6238261 - make this method package protected again - per spec bug 
    void setSource(Object newSource) {
        source = newSource;
    }
}
