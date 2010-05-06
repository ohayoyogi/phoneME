/*
 * @(#)CharToByteMS936.java	1.11 06/10/10
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


package sun.io;

/**
 * Tables and data to convert Unicode to MS936
 *
 * @author  ConverterGenerator tool
 */

public class CharToByteMS936 extends CharToByteGBK {
    public String getCharacterEncoding() {
        return "MS936";
    }

    public CharToByteMS936() {
        super();
    }

    protected int getNative(char ch) {
        int offset = _index1[((ch & 0xff00) >> 8)] << 8;
        int nativeCode = _index2[offset >> 12].charAt((offset & 0xfff) + (ch & 0xff));
        if (nativeCode == 0x0001)
            return (super.getNative(ch));
        else 
            return nativeCode;
    }
    private final static String _innerIndex0 = 
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\uA892\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
        "\u0000\u0000\u0000\u0000\u0000\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
        "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001";
    private final static short _index1[] = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
            2, 3, 4, 5, 6, 7, 8, 9, 10, 0, 0, 0, 0, 0, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
    String _index2[] = {
            _innerIndex0
        };
}
