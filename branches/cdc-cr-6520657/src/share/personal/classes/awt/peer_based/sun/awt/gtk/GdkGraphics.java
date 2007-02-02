/*
 * @(#)GdkGraphics.java	1.36 06/10/10
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

package sun.awt.gtk;

import java.awt.*;
/**
 * GdkGraphics.java
 *
 * @author Nicholas Allen
 */

import java.awt.*;
import java.io.*;
import java.util.*;
import sun.awt.peer.FontPeer;
import java.awt.image.ImageObserver;
import java.awt.image.BufferedImage;
import sun.awt.image.OffScreenImageSource;
import sun.awt.image.ImageRepresentation;
import sun.awt.AWTFinalizer;
import sun.awt.AWTFinalizeable;
import sun.awt.ConstrainableGraphics;
import sun.awt.FontDescriptor;
import sun.awt.PlatformFont;
import sun.awt.CharsetString;
import java.io.CharConversionException;
import sun.io.CharToByteConverter;

/**
 * GdkGraphics is an object that encapsulates a graphics context for a
 * particular drawing area using the Gdk library.
 *
 */

class GdkGraphics extends Graphics2D implements ConstrainableGraphics {
    private static native void initIDs();
    static {
        initIDs();
    }
    int		   data;
    /** The peer that created this graphics. This is needed by clearRect which needs to clear
     he background with the background color of the heavyweight component that created it. */

    GComponentPeer peer;
    Color	   foreground;
    Font  	   font;
    int		   originX;	// TODO: change to float
    int		   originY;	// TODO: change to float
    private AlphaComposite composite;
    /** GTK+ does not support alpha. So alpha is approximated.
     * The approximation is: if alpha is 0.0 then the colour is transparent,
     * else the colour is opaque. This flag indicates whether the extra alpha in
     * the AlphaComposite is not 0.0. The source colour alpha is premultiplied by the
     * extra alpha, hence an extra alpha of 0.0 will create transparent pixels.
     */
    private static final int DRAW_NONE = 0x00;
    private static final int DRAW_PRIMATIVE = 0x01;
    private static final int DRAW_BITMAP = 0x02;
    private static final int DRAW_NORMAL = DRAW_PRIMATIVE | DRAW_BITMAP;
    private static final int DRAW_CLEAR = 0x04 | DRAW_PRIMATIVE;
    private int drawType = DRAW_NORMAL;
    /** The current user clip rectangle or null if no clip has been set. This is stored in the
     native coordinate system and not the possibly translated Java coordinate system. */

    private Rectangle      clip;
    /** The rectangle this graphics object has been constrained too. This is stored in the
     native coordinate system and not the (possibly) translated Java coordinate system.
     If it is null then this graphics has not been constrained. The constrained rectangle
     is another layer of clipping independant of the user clip. The actaul clip used is the
     intersection */

    private Rectangle constrainedRect;
    /** The GraphicsConfiguration that this graphics uses. */
    private GraphicsConfiguration graphicsConfiguration;
    private static native Graphics createFromComponentNative(GComponentPeer peer);

    static Graphics createFromComponent(GComponentPeer peer) {
        Graphics g = createFromComponentNative(peer);
        g.setFont(peer.target.getFont());
        if (g instanceof GdkGraphics)
            ((GdkGraphics) g).setupApproximatedAlpha();
        return g;
    }

    private native void createFromGraphics(GdkGraphics g);

    private native void createFromImage(ImageRepresentation imageRep);

    private native void setForegroundNative(Color c);
    sun.awt.image.Image		image;
    private GdkGraphics (GComponentPeer peer) {
        this.peer = peer;
        composite = AlphaComposite.SrcOver;
        foreground = peer.target.getForeground();
        if (foreground == null)
            foreground = SystemColor.windowText;
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        graphicsConfiguration =
                ge.getDefaultScreenDevice().getDefaultConfiguration();
    }

    private GdkGraphics(GdkGraphics g) {
        createFromGraphics(g);
        graphicsConfiguration = g.graphicsConfiguration;
        peer = g.peer;
        originX = g.originX;
        originY = g.originY;
        clip = g.clip;
        image = g.image;
        composite = g.composite;
        foreground = g.foreground;
        setFont(g.font);
        setupApproximatedAlpha();
    }

    public GdkGraphics(GdkImage image) {
        createFromImage(image.getImageRep());
        composite = AlphaComposite.SrcOver;
        foreground = Color.white;
        graphicsConfiguration = image.gc;
        this.image = image;
        setupApproximatedAlpha();
    }

    /**
     * Create a new GdkGraphics Object based on this one.
     */
    public Graphics create() {
        return new GdkGraphics(this);
    }

    /**
     * Translate
     */
    public void translate(int x, int y) {
        originX += x;
        originY += y;
    }

    public final native void dispose();

    //AWTFinalizeable finalnext;

    public void finalize() {
        /*if (data != 0) {
         AWTFinalizer.addFinalizeable(this);
         }*/
        dispose();
    }

    /*public void doFinalization() {
     dispose();
     }

     public void setNextFinalizeable(AWTFinalizeable o) {
     finalnext = o;
     }

     public AWTFinalizeable getNextFinalizeable() {
     return finalnext;
     }*/

    public void setFont(Font font) {
        if ((font != null) && (this.font != font)) {
            this.font = font;
        }
    }

    public Font getFont() {
        return font;
    }

    /**
     * Gets font metrics for the given font.
     */
    public FontMetrics getFontMetrics(Font font) {
        return (FontMetrics) GFontPeer.getFontPeer(font);
    }

    /**
     * Sets the foreground color.
     */
    public void setColor(Color c) {
        if ((c != null) && (c != foreground)) {
            if (c instanceof SystemColor) {
                c = new Color(c.getRGB());
            }
            foreground = c;
            setupApproximatedAlpha();
        }
    }

    public Color getColor() {
        return foreground;
    }

    public Composite getComposite() {
        return composite;
    }

    public void setComposite(Composite comp) {
        if ((comp != null) && (comp != composite)) {
            if (!(comp instanceof AlphaComposite))
                throw new IllegalArgumentException("Only AlphaComposite is supported");
            composite = (AlphaComposite) comp;
            setupApproximatedAlpha();
        }
    }

    private void setupApproximatedAlpha() {
        int rule = composite.getRule();
        if (rule == AlphaComposite.CLEAR) {
            setForegroundNative(Color.black);
            drawType = DRAW_CLEAR;
        } else if (rule == AlphaComposite.SRC_OVER) {
            if (composite.getAlpha() == 0)
                drawType = DRAW_NONE;
            else if (foreground.getAlpha() == 0)
                drawType = DRAW_BITMAP;
            else {
                drawType = DRAW_NORMAL;
                setForegroundNative(foreground);
            }
        } else {
            drawType = DRAW_NORMAL;
            setForegroundNative(foreground);
        }
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        return graphicsConfiguration;
    }

    /**
     * Sets the paint mode to overwrite the destination with the
     * current color. This is the default paint mode.
     */
    public native void setPaintMode();

    /**
     * Sets the paint mode to alternate between the current color
     * and the given color.
     */
    public native void setXORMode(Color c1);

    /**
     * Gets the current clipping area
     */
    public Rectangle getClipBounds() {
        if (clip != null)
            return new Rectangle (clip.x - originX, clip.y - originY, clip.width, clip.height);
        return null;
    }

    /** Returns a Shape object representing the MicroWindows clip. */

    public Shape    getClip() {
        return getClipBounds();
    }

    public void constrain(int x, int y, int w, int h) {
        originX += x;
        originY += y;
        Rectangle rect = new Rectangle(originX, originY, w, h);
        if (constrainedRect != null)
            constrainedRect = constrainedRect.intersection(rect);
        else constrainedRect = rect;
        setupClip();
    }

    private native void setClipNative(int X, int Y, int W, int H);

    private native void removeClip();

    public void     clipRect(int x, int y, int w, int h) {
        Rectangle rect = new Rectangle(x + originX, y + originY, w, h);
        if (clip != null)
            clip = clip.intersection(rect);
        else
            clip = rect;
        setupClip();
    }

    /** Sets up the clip for this Graphics. The clip is the result of combining the user clip
     with the constrainedRect. */

    private void setupClip() {
        if (constrainedRect != null) {
            if (clip != null) {
                Rectangle intersection = constrainedRect.intersection(clip);
                setClipNative(intersection.x, intersection.y, intersection.width, intersection.height);
            } else {
                setClipNative(constrainedRect.x, constrainedRect.y, constrainedRect.width, constrainedRect.height);
            }
        } else {
            if (clip != null) {
                setClipNative(clip.x, clip.y, clip.width, clip.height);
            } else {
                removeClip();
            }
        }
    }

    /** Sets the clipping rectangle for this X11Graphics context. */
    public void     setClip(int x, int y, int w, int h) {
        clip = new Rectangle (x + originX, y + originY, w, h);
        setupClip();
    }

    /** Sets the clip to a Shape (only Rectangle allowed). */

    public void     setClip(Shape clip) {
        if (clip == null) {
            this.clip = null;
            setupClip();
        } else if (clip instanceof Rectangle) {
            Rectangle rect = (Rectangle) clip;
            setClip(rect.x, rect.y, rect.width, rect.height);
        } else
            throw new IllegalArgumentException("setClip(Shape) only supports Rectangle objects");
    }

    /** Clears the rectangle indicated by x,y,w,h. */
    public native void clearRect(int x, int y, int w, int h);

    /** Fills the given rectangle with the foreground color. */
    public void fillRect(int X, int Y, int W, int H) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  drawRectNative(true, X, Y, W, H);
    }

    /** Draws the given rectangle. */
    public void drawRect(int X, int Y, int W, int H) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  drawRectNative(false, X, Y, W, H);
    }

    private native void drawRectNative(boolean fill, int X, int Y, int W, int H);

    /** Draws the given string. */
    public void drawString(String string, int x, int y) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE) {
			GFontPeer p = GFontPeer.getFontPeer(font);
			CharsetString[] cs = p.gpf.makeMultiCharsetString(string);
			
			for(int i=0;i<cs.length;i++)
			{
				byte[] s = new byte[cs[i].length*3];
				int len;
				
				try {
					len = cs[i].fontDescriptor.fontCharset.convertAny(cs[i].charsetChars, cs[i].offset, cs[i].length, s, 0, s.length);
				} catch(Exception e)
				{
					/* FIXME ... */
					continue;
				}
					
				int gdkfont = p.gpf.getGdkFont(cs[i].fontDescriptor);
				
//				drawStringNative(string.getBytes(), x, y);
				drawStringNative(s, len, gdkfont, x, y);
				x+= p.stringWidthNative(s, len, gdkfont);
			}
			
	
		}
    }

    private native void drawStringNative(byte[] string, int len, int gdkfont, int x, int y);

    /** Draws the given character array. */
    public void drawChars(char chars[], int offset, int length, int x, int y) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  
			drawString(new String (chars, offset, length), x, y);
    }

    /** Draws the given byte array. */
    public void drawBytes(byte data[], int offset, int length, int x, int y) {
        if (drawType == DRAW_NONE) return;

/* FIXME        
		byte[] string = new byte[length];
        System.arraycopy(data, offset, string, 0, length);
        drawStringNative(GFontPeer.getFontPeer(font), string, x, y);
*/
    }

    private native void drawLineNative(int x1, int y1, int x2, int y2);

    /** Draws the given line. */
    public void drawLine(int x1, int y1, int x2, int y2) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  drawLineNative(x1, y1, x2, y2);
    }

    /**
     * Draws an image at x,y in nonblocking mode with a callback object.
     */
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        GdkDrawableImage gdkimage;
        if (img instanceof BufferedImage)
            gdkimage = (GdkDrawableImage) GToolkit.getBufferedImagePeer((BufferedImage) img);
        else gdkimage = (GdkDrawableImage) img;
        switch (drawType) {
        case DRAW_BITMAP:
        case DRAW_NORMAL:
            return gdkimage.draw(this, x, y, observer);

        case DRAW_CLEAR:
            drawRectNative(true, x, y, gdkimage.getWidth(), gdkimage.getHeight());

        case DRAW_NONE:
        default:
            return gdkimage.isComplete();
        }
    }

    /**
     * Draws an image scaled to x,y,w,h in nonblocking mode with a
     * callback object.
     */
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        GdkDrawableImage gdkimage;
        if (img instanceof BufferedImage)
            gdkimage = (GdkDrawableImage) GToolkit.getBufferedImagePeer((BufferedImage) img);
        else gdkimage = (GdkDrawableImage) img;
        switch (drawType) {
        case DRAW_BITMAP:
        case DRAW_NORMAL:
            return gdkimage.draw(this, x, y, width, height, observer);

        case DRAW_CLEAR:
            drawRectNative(true, x, y, width, height);

        case DRAW_NONE:
        default:
            return gdkimage.isComplete();
        }
    }

    /**
     * Draws an image at x,y in nonblocking mode with a solid background
     * color and a callback object.
     */
    public boolean drawImage(Image img, int x, int y, Color bg, ImageObserver observer) {
        GdkDrawableImage gdkimage;
        if (img instanceof BufferedImage)
            gdkimage = (GdkDrawableImage) GToolkit.getBufferedImagePeer((BufferedImage) img);
        else gdkimage = (GdkDrawableImage) img;
        switch (drawType) {
        case DRAW_BITMAP:
        case DRAW_NORMAL:
            return gdkimage.draw(this, x, y, bg, observer);

        case DRAW_CLEAR:
            drawRectNative(true, x, y, gdkimage.getWidth(), gdkimage.getHeight());

        case DRAW_NONE:
        default:
            return gdkimage.isComplete();
        }
    }

    /**
     * Draws an image scaled to x,y,w,h in nonblocking mode with a
     * solid background color and a callback object.
     */
    public boolean drawImage(Image img, int x, int y, int width, int height,
        Color bg, ImageObserver observer) {
        GdkDrawableImage gdkimage;
        if (img instanceof BufferedImage)
            gdkimage = (GdkDrawableImage) GToolkit.getBufferedImagePeer((BufferedImage) img);
        else gdkimage = (GdkDrawableImage) img;
        switch (drawType) {
        case DRAW_BITMAP:
        case DRAW_NORMAL:
            return gdkimage.draw(this, x, y, width, height, bg, observer);

        case DRAW_CLEAR:
            drawRectNative(true, x, y, width, height);

        case DRAW_NONE:
        default:
            return gdkimage.isComplete();
        }
    }

    /**
     * Draws a subrectangle of an image scaled to a destination rectangle
     * in nonblocking mode with a callback object.
     */
    public boolean drawImage(Image img,
        int dx1, int dy1, int dx2, int dy2,
        int sx1, int sy1, int sx2, int sy2,
        ImageObserver observer) {
        GdkDrawableImage gdkimage;
        if (img instanceof BufferedImage)
            gdkimage = (GdkDrawableImage) GToolkit.getBufferedImagePeer((BufferedImage) img);
        else gdkimage = (GdkDrawableImage) img;
        switch (drawType) {
        case DRAW_BITMAP:
        case DRAW_NORMAL:
            return gdkimage.draw(this, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);

        case DRAW_CLEAR: {
                int w = dx2 - dx1;
                int h = dy2 - dy1;
                int x = dx1, y = dy1;
                if (w < 0) {
                    x = dx2;
                    w *= -1;
                }
                if (h < 0) {
                    y = dy2;
                    h *= -1;
                }
                drawRectNative(true, x, y, w, h);
            }

        case DRAW_NONE:
        default:
            return gdkimage.isComplete();
        }
    }

    /**
     * Draws a subrectangle of an image scaled to a destination rectangle in
     * nonblocking mode with a solid background color and a callback object.
     */
    public boolean drawImage(Image img,
        int dx1, int dy1, int dx2, int dy2,
        int sx1, int sy1, int sx2, int sy2,
        Color bgcolor, ImageObserver observer) {
        GdkDrawableImage gdkimage;
        if (img instanceof BufferedImage)
            gdkimage = (GdkDrawableImage) GToolkit.getBufferedImagePeer((BufferedImage) img);
        else gdkimage = (GdkDrawableImage) img;
        switch (drawType) {
        case DRAW_BITMAP:
        case DRAW_NORMAL:
            return gdkimage.draw(this, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);

        case DRAW_CLEAR: {
                int w = dx2 - dx1;
                int h = dy2 - dy1;
                int x = dx1, y = dy1;
                if (w < 0) {
                    x = dx2;
                    w *= -1;
                }
                if (h < 0) {
                    y = dy2;
                    h *= -1;
                }
                drawRectNative(true, x, y, w, h);
            }

        case DRAW_NONE:
        default:
            return gdkimage.isComplete();
        }
    }

    /**
     * Copies an area of the canvas that this graphics context paints to.
     * @param X the x-coordinate of the source.
     * @param Y the y-coordinate of the source.
     * @param W the width.
     * @param H the height.
     * @param dx the x-coordinate of the destination.
     * @param dy the y-coordinate of the destination.
     */
    public native void copyArea(int X, int Y, int W, int H, int dx, int dy);

    private native void drawRoundRectNative(int x, int y, int w, int h, int arcWidth, int arcHeight);

    private native void fillRoundRectNative(int x, int y, int w, int h, int arcWidth, int arcHeight);

    /** Draws a rounded rectangle. */
    public void drawRoundRect(int x, int y, int w, int h, int arcWidth, int arcHeight) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  drawRoundRectNative(x, y, w, h, arcWidth, arcHeight);
    }

    /** Draws a filled rounded rectangle. */
    public void fillRoundRect(int x, int y, int w, int h, int arcWidth, int arcHeight) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  fillRoundRectNative(x, y, w, h, arcWidth, arcHeight);
    }

    /** Draws a bunch of lines defined by an array of x points and y points */
    private native void drawPolygonNative(boolean filled, int xPoints[], int yPoints[],
        int nPoints, boolean close);

    /** Draws a bunch of lines defined by an array of x points and y points */
    public void drawPolyline(int xPoints[], int yPoints[], int nPoints) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  drawPolygonNative(false, xPoints, yPoints, nPoints, false);
    }

    /** Draws a polygon defined by an array of x points and y points */
    public void drawPolygon(int xPoints[], int yPoints[], int nPoints) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  drawPolygonNative(false, xPoints, yPoints, nPoints, true);
    }

    /** Fills a polygon with the current fill mask */
    public void fillPolygon(int xPoints[], int yPoints[], int nPoints) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  drawPolygonNative(true, xPoints, yPoints, nPoints, false);
    }

    /** Draws an oval to fit in the given rectangle */
    public void drawOval(int x, int y, int w, int h) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  drawArc(x, y, w, h, 0, 360);
    }

    /** Fills an oval to fit in the given rectangle */
    public void fillOval(int x, int y, int w, int h) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  fillArc(x, y, w, h, 0, 360);
    }

    /**
     * Draws an arc bounded by the given rectangle from startAngle to
     * endAngle. 0 degrees is a vertical line straight up from the
     * center of the rectangle. Positive angles indicate clockwise
     * rotations, negative angle are counter-clockwise.
     */
    public void drawArc(int x, int y, int w, int h, int startAngle, int endAngle) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  drawArcNative(x, y, w, h, startAngle, endAngle);
    }

    /** fills an arc. arguments are the same as drawArc. */
    public void fillArc(int x, int y, int w, int h, int startAngle, int endAngle) {
        if ((drawType & DRAW_PRIMATIVE) == DRAW_PRIMATIVE)  fillArcNative(x, y, w, h, startAngle, endAngle);
    }

    private native void drawArcNative(int x, int y, int w, int h, int startAngle, int endAngle);

    private native void fillArcNative(int x, int y, int w, int h, int startAngle, int endAngle);

    public String toString() {
        return getClass().getName() + "[" + originX + "," + originY + "]";
    }
    /* Outline the given region. */
    //public native void drawRegion(Region r);

    /* Fill the given region. */
    //public native void fillRegion(Region r);
	
	
	public void setStroke(Stroke stroke) { }
	public Stroke getStroke() { return null; }

}
