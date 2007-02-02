/*
 * @(#)ImageDecoder.java	1.22 06/10/10
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

package sun.awt.image;

import java.util.Hashtable;
import java.io.InputStream;
import java.io.IOException;
import java.awt.image.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

public abstract class ImageDecoder {
    InputStreamImageSource source;
    InputStream input;
    Thread feeder;
    protected boolean aborted;
    protected boolean finished;
    ImageConsumerQueue queue;
    ImageDecoder next;
    public ImageDecoder(InputStreamImageSource src, InputStream is) {
        source = src;
        input = is;
        feeder = Thread.currentThread();
    }

    public boolean isConsumer(ImageConsumer ic) {
        return ImageConsumerQueue.isConsumer(queue, ic);
    }

    public void removeConsumer(ImageConsumer ic) {
        queue = ImageConsumerQueue.removeConsumer(queue, ic, false);
        if (!finished && queue == null) {
            abort();
        }
    }

    protected ImageConsumerQueue nextConsumer(ImageConsumerQueue cq) {
        synchronized (source) {
            if (aborted) {
                return null;
            }
            cq = ((cq == null) ? queue : cq.next);
            while (cq != null) {
                if (cq.interested) {
                    return cq;
                }
                cq = cq.next;
            }
        }
        return null;
    }

    protected int setDimensions(int w, int h) {
        ImageConsumerQueue cq = null;
        int count = 0;
        while ((cq = nextConsumer(cq)) != null) {
            cq.consumer.setDimensions(w, h);
            count++;
        }
        return count;
    }

    protected int setProperties(Hashtable props) {
        ImageConsumerQueue cq = null;
        int count = 0;
        while ((cq = nextConsumer(cq)) != null) {
            cq.consumer.setProperties(props);
            count++;
        }
        return count;
    }

    protected int setColorModel(ColorModel model) {
        ImageConsumerQueue cq = null;
        int count = 0;
        while ((cq = nextConsumer(cq)) != null) {
            cq.consumer.setColorModel(model);
            count++;
        }
        return count;
    }

    protected int setHints(int hints) {
        ImageConsumerQueue cq = null;
        int count = 0;
        while ((cq = nextConsumer(cq)) != null) {
            cq.consumer.setHints(hints);
            count++;
        }
        return count;
    }

    protected void headerComplete() {
        feeder.setPriority(ImageFetcher.LOW_PRIORITY);
    }

    protected int setPixels(int x, int y, int w, int h, ColorModel model,
        byte pix[], int off, int scansize) {
        source.latchConsumers(this);
        ImageConsumerQueue cq = null;
        int count = 0;
        while ((cq = nextConsumer(cq)) != null) {
            cq.consumer.setPixels(x, y, w, h, model, pix, off, scansize);
            count++;
        }
        return count;
    }

    protected int setPixels(int x, int y, int w, int h, ColorModel model,
        int pix[], int off, int scansize) {
        source.latchConsumers(this);
        ImageConsumerQueue cq = null;
        int count = 0;
        while ((cq = nextConsumer(cq)) != null) {
            cq.consumer.setPixels(x, y, w, h, model, pix, off, scansize);
            count++;
        }
        return count;
    }

    protected int imageComplete(int status, boolean done) {
        source.latchConsumers(this);
        if (done) {
            finished = true;
            source.doneDecoding(this);
        }
        ImageConsumerQueue cq = null;
        int count = 0;
        while ((cq = nextConsumer(cq)) != null) {
            cq.consumer.imageComplete(status);
            count++;
        }
        return count;
    }

// J2SE
// Fixed 4522041: Redundant image data increases footprint
/*
    public void replayConsumer(ImageConsumer ic) {}

    public abstract boolean catchupConsumer(InputStreamImageSource src,
        ImageConsumer ic);
*/

    public abstract void produceImage() throws IOException,
            ImageFormatException;

    public void abort() {
        aborted = true;
        source.doneDecoding(this);
        close();
        AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {
                    feeder.interrupt();
                    return null;
                }
            }
        );
    }

    public synchronized void close() {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {}
        }
    }
}
