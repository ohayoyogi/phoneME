/*
 * %W% %E%
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

package com.sun.jump.module.installer;

import com.sun.jump.common.JUMPAppModel;
import com.sun.jump.module.JUMPModuleFactory;

/**
 * <code>JUMPInstallerModuleFactory</code> is a factory for 
 * <code>JUMPInstallerModule</code>
 */
public abstract class JUMPInstallerModuleFactory extends JUMPModuleFactory {
    private static JUMPInstallerModuleFactory INSTANCE = null;
    
    public static JUMPInstallerModuleFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * Creates a new instance of JUMPInstallerModuleFactory
     */
    protected JUMPInstallerModuleFactory() {
        synchronized (JUMPInstallerModuleFactory.class){
            if ( INSTANCE == null ) {
                INSTANCE = this;
            }
        }
    }
    
    /**
     * Returns a <code>JUMPInstallerModule</code> for the app model specified
     * 
     * @param appModel the application model for which an appropriate
     *        installer module should be returned.
     */
    public abstract JUMPInstallerModule getModule(JUMPAppModel appModel);

    /**
     * Returns a <code>JUMPInstallerModule</code> for the mime type 
     * specified.
     *
     * @param mimeType mime type for which an appropriate
     *        installer module should be returned. 
     * 
     * @return This can return null if no installer module is available
     *        
     */
    public abstract JUMPInstallerModule getModule(String mimeType);
    
    /**
     * @return a list of all registered installers in the system for all types
     * of content.
     */
    public abstract JUMPInstallerModule[] getAllInstallers();
}
