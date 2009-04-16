/*
 * @(#)Button.java	1.57 06/10/10
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

package java.awt;

import sun.awt.peer.ButtonPeer;
import sun.awt.PeerBasedToolkit;
import java.awt.event.*;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.EventListener;

/**
 * This class creates a labeled button. The application can cause
 * some action to happen when the button is pushed. This image
 * depicts three views of a "<code>Quit</code>" button as it appears
 * under the Solaris operating system:
 * <p>
 * <img src="images-awt/Button-1.gif"
 * ALIGN=center HSPACE=10 VSPACE=7>
 * <p>
 * The first view shows the button as it appears normally.
 * The second view shows the button
 * when it has input focus. Its outline is darkened to let the
 * user know that it is an active object. The third view shows the
 * button when the user clicks the mouse over the button, and thus
 * requests that an action be performed.
 * <p>
 * The gesture of clicking on a button with the mouse
 * is associated with one instance of <code>ActionEvent</code>,
 * which is sent out when the mouse is both pressed and released
 * over the button. If an application is interested in knowing
 * when the button has been pressed but not released, as a separate
 * gesture, it can specialize <code>processMouseEvent</code>,
 * or it can register itself as a listener for mouse events by
 * calling <code>addMouseListener</code>. Both of these methods are
 * defined by <code>Component</code>, the abstract superclass of
 * all components.
 * <p>
 * When a button is pressed and released, AWT sends an instance
 * of <code>ActionEvent</code> to the button, by calling
 * <code>processEvent</code> on the button. The button's
 * <code>processEvent</code> method receives all events
 * for the button; it passes an action event along by
 * calling its own <code>processActionEvent</code> method.
 * The latter method passes the action event on to any action
 * listeners that have registered an interest in action
 * events generated by this button.
 * <p>
 * If an application wants to perform some action based on
 * a button being pressed and released, it should implement
 * <code>ActionListener</code> and register the new listener
 * to receive events from this button, by calling the button's
 * <code>addActionListener</code> method. The application can
 * make use of the button's action command as a messaging protocol.
 *
 * @version 	1.39 06/12/97
 * @author 	Sami Shaio
 * @see         java.awt.event.ActionEvent
 * @see         java.awt.event.ActionListener
 * @see         java.awt.Component#processMouseEvent
 * @see         java.awt.Component#addMouseListener
 * @since       JDK1.0
 */
public class Button extends Component {
    String label;
    String actionCommand;
    transient ActionListener actionListener;
    private static final String base = "button";
    private static int nameCounter = 0;
    /*
     * JDK 1.1 serialVersionUID
     */
    private static final long serialVersionUID = -8774683716313001058L;
    /**
     * Constructs a Button with no label.
     * @since JDK1.0
     */
    public Button() {
        this("");
    }

    /**
     * Constructs a Button with the specified label.
     * @param label A string label for the button.
     * @since JDK1.0
     */
    public Button(String label) {
        this.label = label;
    }

    /**
     * Construct a name for this component.  Called by getName() when the
     * name is null.
     */
    String constructComponentName() {
        return base + nameCounter++;
    }

    /**
     * Creates the peer of the button.  The button's peer allows the
     * application to change the look of the button without changing
     * its functionality.
     * @see     java.awt.Toolkit#createButton(java.awt.Button)
     * @see     java.awt.Component#getToolkit()
     * @since   JDK1.0
     */
    public void addNotify() {
        synchronized (getTreeLock()) {
            if (peer == null)
                peer = ((PeerBasedToolkit) getToolkit()).createButton(this);
            super.addNotify();
        }
    }

    /**
     * Gets the label of this button.
     * @return    the button's label, or <code>null</code>
     *                if the button has no label.
     * @see       java.awt.Button#setLabel
     * @since     JDK1.0
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the button's label to be the specified string.
     * @param     label   the new label, or <code>null</code>
     *                if the button has no label.
     * @see       java.awt.Button#getLabel
     * @since     JDK1.0
     */
    public synchronized void setLabel(String label) {
        this.label = label;
        ButtonPeer peer = (ButtonPeer) this.peer;
        if (peer != null) {
            peer.setLabel(label);
        }
    }

    /**
     * Sets the command name for the action event fired
     * by this button. By default this action command is
     * set to match the label of the button.
     * @param     command  A string used to set the button's
     *                  action command.
     * @see       java.awt.event.ActionEvent
     * @since     JDK1.1
     */
    public void setActionCommand(String command) {
        actionCommand = command;
    }

    /**
     * Returns the command name of the action event fired by this button.
     */
    public String getActionCommand() {
        return (actionCommand == null ? label : actionCommand);
    }

    /**
     * Adds the specified action listener to receive action events from
     * this button. Action events occur when a user presses or releases
     * the mouse over this button.
     * @param         l the action listener.
     * @see           java.awt.event.ActionListener
     * @see           java.awt.Button#removeActionListener
     * @since         JDK1.1
     */
    public synchronized void addActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.add(actionListener, l);
        newEventsOnly = true;
    }

    /**
     * Removes the specified action listener so that it no longer
     * receives action events from this button. Action events occur
     * when a user presses or releases the mouse over this button.
     * @param         l     the action listener.
     * @see           java.awt.event.ActionListener
     * @see           java.awt.Button#addActionListener
     * @since         JDK1.1
     */
    public synchronized void removeActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.remove(actionListener, l);
    }

    /**
     * Returns an array of all the action listeners
     * registered on this button.
     *
     * @return all of this button's <code>ActionListener</code>s
     *         or an empty array if no action
     *         listeners are currently registered
     *
     * @see             #addActionListener
     * @see             #removeActionListener
     * @see             java.awt.event.ActionListener
     * @since 1.4
     */
    public synchronized ActionListener[] getActionListeners() {
        return (ActionListener[]) AWTEventMulticaster.getListeners(
                                  (EventListener)actionListener,
                                  ActionListener.class);
    }

    // NOTE: remove when filtering is done at lower level
    boolean eventEnabled(AWTEvent e) {
        if (e.id == ActionEvent.ACTION_PERFORMED) {
            if ((eventMask & AWTEvent.ACTION_EVENT_MASK) != 0 ||
                actionListener != null) {
                return true;
            }
            return false;
        }
        return super.eventEnabled(e);
    }

    /**
     * Processes events on this button. If an event is
     * an instance of <code>ActionEvent</code>, this method invokes
     * the <code>processActionEvent</code> method. Otherwise,
     * it invokes <code>processEvent</code> on the superclass.
     * @param        e the event.
     * @see          java.awt.event.ActionEvent
     * @see          java.awt.Button#processActionEvent
     * @since        JDK1.1
     */
    protected void processEvent(AWTEvent e) {
        if (e instanceof ActionEvent) {
            processActionEvent((ActionEvent) e);
            return;
        }
        super.processEvent(e);
    }

    /**
     * Processes action events occurring on this button
     * by dispatching them to any registered
     * <code>ActionListener</code> objects.
     * <p>
     * This method is not called unless action events are
     * enabled for this button. Action events are enabled
     * when one of the following occurs:
     * <p><ul>
     * <li>An <code>ActionListener</code> object is registered
     * via <code>addActionListener</code>.
     * <li>Action events are enabled via <code>enableEvents</code>.
     * </ul>
     * @param       e the action event.
     * @see         java.awt.event.ActionListener
     * @see         java.awt.Button#addActionListener
     * @see         java.awt.Component#enableEvents
     * @since       JDK1.1
     */
    protected void processActionEvent(ActionEvent e) {
        if (actionListener != null) {
            actionListener.actionPerformed(e);
        }
    }

    /**
     * Returns the parameter string representing the state of this
     * button. This string is useful for debugging.
     * @return     the parameter string of this button.
     * @since      JDK1.0
     */
    protected String paramString() {
        return super.paramString() + ",label=" + label;
    }
    /* Serialization support.
     */

    private int buttonSerializedDataVersion = 1;
    private void writeObject(ObjectOutputStream s)
        throws IOException {
        s.defaultWriteObject();
        AWTEventMulticaster.save(s, actionListenerK, actionListener);
        s.writeObject(null);
    }

    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException {
        s.defaultReadObject();
        Object keyOrNull;
        while (null != (keyOrNull = s.readObject())) {
            String key = ((String) keyOrNull).intern();
            if (actionListenerK == key)
                addActionListener((ActionListener) (s.readObject()));
            else // skip value for unrecognized key
                s.readObject();
        }
    }
}
