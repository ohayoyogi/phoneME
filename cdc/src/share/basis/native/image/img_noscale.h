/*
 * @(#)img_noscale.h	1.13 06/10/10
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

/*
 * This file contains macro definitions for the Scaling category of
 * the macros used by the generic scaleloop function.
 *
 * This implementation performs no input resampling whatsoever and
 * is only valid if the input data is delivered at the exact same
 * resolution as the output data is being generated.  At the same
 * time, this implementation of the Scaling macros is the most optimal
 * such implementation.
 */

#define DeclareScaleVars					\
    int dstX, dstY, dstX2, dstY2;

#define SRCX	dstX
#define SRCY	dstY
#define DSTX	dstX
#define DSTY	dstY
#define DSTX1	srcOX
#define DSTY1	srcOY
#define DSTX2	dstX2
#define DSTY2	dstY2

#define InitScale(pixels, srcOff, srcScan,				\
		  srcOX, srcOY, srcW, srcH,				\
		  srcTW, srcTH, dstTW, dstTH)				\
    do {								\
	dstX2 = srcOX + srcW;						\
	dstY2 = srcOY + srcH;						\
	SetInputRow(pixels, srcOff, srcScan, srcOY, srcOY);		\
    } while (0)

#define RowLoop(srcOY)							\
    for (dstY = srcOY; dstY < dstY2; dstY++)

#define RowSetup(srcTH, dstTH, srcTW, dstTW,				\
		 srcOY, pixels, srcOff, srcScan)			\
	do {} while (0)

#define ColLoop(srcOX)							\
	for (dstX = srcOX; dstX < dstX2; dstX++)

#define ColSetup(srcTW, dstTW, pixel)					\
	    pixel = GetPixelInc()

#define RowEnd(srcTH, dstTH, srcW, srcScan)				\
	InputPixelInc(srcScan - srcW)
