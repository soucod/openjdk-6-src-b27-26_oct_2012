/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.awt.X11;

import java.awt.Image;

import java.awt.datatransfer.DataFlavor;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import java.io.InputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;

import sun.awt.datatransfer.DataTransferer;
import sun.awt.datatransfer.ToolkitThreadBlockedHandler;

/**
 * Platform-specific support for the data transfer subsystem.
 */
public class XDataTransferer extends DataTransferer {
    static final XAtom FILE_NAME_ATOM = XAtom.get("FILE_NAME");
    static final XAtom DT_NET_FILE_ATOM = XAtom.get("_DT_NETFILE");
    static final XAtom PNG_ATOM = XAtom.get("PNG");
    static final XAtom JFIF_ATOM = XAtom.get("JFIF");
    static final XAtom TARGETS_ATOM = XAtom.get("TARGETS");
    static final XAtom INCR_ATOM = XAtom.get("INCR");
    static final XAtom MULTIPLE_ATOM = XAtom.get("MULTIPLE");

    /**
     * Singleton constructor
     */
    private XDataTransferer() {
    }

    private static XDataTransferer transferer;

    static XDataTransferer getInstanceImpl() {
        synchronized (XDataTransferer.class) {
            if (transferer == null) {
                transferer = new XDataTransferer();
            }
        }
        return transferer;
    }

    public String getDefaultUnicodeEncoding() {
        return "iso-10646-ucs-2";
    }

    public boolean isLocaleDependentTextFormat(long format) {
        return false;
    }

    public boolean isTextFormat(long format) {
        return super.isTextFormat(format)
            || isMimeFormat(format, "text");
    }

    protected String getCharsetForTextFormat(Long lFormat) {
        long format = lFormat.longValue();
        if (isMimeFormat(format, "text")) {
            String nat = getNativeForFormat(format);
            DataFlavor df = new DataFlavor(nat, null);
            // Ignore the charset parameter of the MIME type if the subtype
            // doesn't support charset.
            if (!DataTransferer.doesSubtypeSupportCharset(df)) {
                return null;
            }
            String charset = df.getParameter("charset");
            if (charset != null) {
                return charset;
            }
        }
        return super.getCharsetForTextFormat(lFormat);
    }

    public boolean isFileFormat(long format) {
        return format == FILE_NAME_ATOM.getAtom() ||
            format == DT_NET_FILE_ATOM.getAtom();
    }

    public boolean isImageFormat(long format) {
        return format == PNG_ATOM.getAtom() ||
            format == JFIF_ATOM.getAtom() ||
            isMimeFormat(format, "image");
    }

    protected Long getFormatForNativeAsLong(String str) {
        // Just get the atom. If it has already been retrived
        // once, we'll get a copy so this should be very fast.
        long atom = XAtom.get(str).getAtom();
        return Long.valueOf(atom);
    }

    protected String getNativeForFormat(long format) {
        return getTargetNameForAtom(format);
    }

    public ToolkitThreadBlockedHandler getToolkitThreadBlockedHandler() {
        return XToolkitThreadBlockedHandler.getToolkitThreadBlockedHandler();
    }

    /**
     * Gets an format name for a given format (atom)
     */
    private String getTargetNameForAtom(long atom) {
        return XAtom.get(atom).getName();
    }

    protected byte[] imageToPlatformBytes(Image image, long format)
      throws IOException {
        String mimeType = null;
        if (format == PNG_ATOM.getAtom()) {
            mimeType = "image/png";
        } else if (format == JFIF_ATOM.getAtom()) {
            mimeType = "image/jpeg";
        } else {
            // Check if an image MIME format.
            try {
                String nat = getNativeForFormat(format);
                DataFlavor df = new DataFlavor(nat);
                String primaryType = df.getPrimaryType();
                if ("image".equals(primaryType)) {
                    mimeType = df.getPrimaryType() + "/" + df.getSubType();
                }
            } catch (Exception e) {
                // Not an image MIME format.
            }
        }
        if (mimeType != null) {
            return imageToStandardBytes(image, mimeType);
        } else {
            String nativeFormat = getNativeForFormat(format);
            throw new IOException("Translation to " + nativeFormat +
                                  " is not supported.");
        }
    }

    /**
     * Translates either a byte array or an input stream which contain
     * platform-specific image data in the given format into an Image.
     */
    protected Image platformImageBytesOrStreamToImage(InputStream inputStream,
                                                      byte[] bytes,
                                                      long format)
      throws IOException {
        String mimeType = null;
        if (format == PNG_ATOM.getAtom()) {
            mimeType = "image/png";
        } else if (format == JFIF_ATOM.getAtom()) {
            mimeType = "image/jpeg";
        } else {
            // Check if an image MIME format.
            try {
                String nat = getNativeForFormat(format);
                DataFlavor df = new DataFlavor(nat);
                String primaryType = df.getPrimaryType();
                if ("image".equals(primaryType)) {
                    mimeType = df.getPrimaryType() + "/" + df.getSubType();
                }
            } catch (Exception e) {
                // Not an image MIME format.
            }
        }
        if (mimeType != null) {
            return standardImageBytesOrStreamToImage(inputStream, bytes, mimeType);
        } else {
            String nativeFormat = getNativeForFormat(format);
            throw new IOException("Translation from " + nativeFormat +
                                  " is not supported.");
        }
    }

    protected String[] dragQueryFile(byte[] bytes) {
        XToolkit.awtLock();
        try {
            return XlibWrapper.XTextPropertyToStringList(bytes,
                                                         XAtom.get("STRING").getAtom());
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /**
     * Returns true if and only if the name of the specified format Atom
     * constitutes a valid MIME type with the specified primary type.
     */
    private boolean isMimeFormat(long format, String primaryType) {
        String nat = getNativeForFormat(format);

        if (nat == null) {
            return false;
        }

        try {
            DataFlavor df = new DataFlavor(nat);
            if (primaryType.equals(df.getPrimaryType())) {
                return true;
            }
        } catch (Exception e) {
            // Not a MIME format.
        }

        return false;
    }

    /*
     * The XDnD protocol prescribes that the Atoms used as targets for data
     * transfer should have string names that represent the corresponding MIME
     * types.
     * To meet this requirement we check if the passed native format constitutes
     * a valid MIME and return a list of flavors to which the data in this MIME
     * type can be translated by the Data Transfer subsystem.
     */
    public List getPlatformMappingsForNative(String nat) {
        List flavors = new ArrayList();

        if (nat == null) {
            return flavors;
        }

        DataFlavor df = null;

        try {
            df = new DataFlavor(nat);
        } catch (Exception e) {
            // The string doesn't constitute a valid MIME type.
            return flavors;
        }

        Object value = df;
        final String primaryType = df.getPrimaryType();
        final String baseType = primaryType + "/" + df.getSubType();

        // For text formats we map natives to MIME strings instead of data
        // flavors to enable dynamic text native-to-flavor mapping generation.
        // See SystemFlavorMap.getFlavorsForNative() for details.
        if ("text".equals(primaryType)) {
            value = primaryType + "/" + df.getSubType();
        } else if ("image".equals(primaryType)) {
            Iterator readers = ImageIO.getImageReadersByMIMEType(baseType);
            if (readers.hasNext()) {
                flavors.add(DataFlavor.imageFlavor);
            }
        }

        flavors.add(value);

        return flavors;
    }

    private static ImageTypeSpecifier defaultSpecifier = null;

    private ImageTypeSpecifier getDefaultImageTypeSpecifier() {
        if (defaultSpecifier == null) {
            ColorModel model = ColorModel.getRGBdefault();
            WritableRaster raster =
                model.createCompatibleWritableRaster(10, 10);

            BufferedImage bufferedImage =
                new BufferedImage(model, raster, model.isAlphaPremultiplied(),
                                  null);

            defaultSpecifier = new ImageTypeSpecifier(bufferedImage);
        }

        return defaultSpecifier;
    }

    /*
     * The XDnD protocol prescribes that the Atoms used as targets for data
     * transfer should have string names that represent the corresponding MIME
     * types.
     * To meet this requirement we return a list of formats that represent
     * MIME types to which the data in this flavor can be translated by the Data
     * Transfer subsystem.
     */
    public List getPlatformMappingsForFlavor(DataFlavor df) {
        List natives = new ArrayList(1);

        if (df == null) {
            return natives;
        }

        String charset = df.getParameter("charset");
        String baseType = df.getPrimaryType() + "/" + df.getSubType();
        String mimeType = baseType;

        if (charset != null && DataTransferer.isFlavorCharsetTextType(df)) {
            mimeType += ";charset=" + charset;
        }

        // Add a mapping to the MIME native whenever the representation class
        // doesn't require translation.
        if (df.getRepresentationClass() != null &&
            (df.isRepresentationClassInputStream() ||
             df.isRepresentationClassByteBuffer() ||
             byteArrayClass.equals(df.getRepresentationClass()))) {
            natives.add(mimeType);
        }

        if (DataFlavor.imageFlavor.equals(df)) {
            String[] mimeTypes = ImageIO.getWriterMIMETypes();
            if (mimeTypes != null) {
                for (int i = 0; i < mimeTypes.length; i++) {
                    Iterator writers =
                        ImageIO.getImageWritersByMIMEType(mimeTypes[i]);

                    while (writers.hasNext()) {
                        ImageWriter imageWriter = (ImageWriter)writers.next();
                        ImageWriterSpi writerSpi =
                            imageWriter.getOriginatingProvider();

                        if (writerSpi != null &&
                            writerSpi.canEncodeImage(getDefaultImageTypeSpecifier())) {
                            natives.add(mimeTypes[i]);
                            break;
                        }
                    }
                }
            }
        } else if (DataTransferer.isFlavorCharsetTextType(df)) {
            final Iterator iter = DataTransferer.standardEncodings();

            // stringFlavor is semantically equivalent to the standard
            // "text/plain" MIME type.
            if (DataFlavor.stringFlavor.equals(df)) {
                baseType = "text/plain";
            }

            while (iter.hasNext()) {
                String encoding = (String)iter.next();
                if (!encoding.equals(charset)) {
                    natives.add(baseType + ";charset=" + encoding);
                }
            }

            // Add a MIME format without specified charset.
            if (!natives.contains(baseType)) {
                natives.add(baseType);
            }
        }

        return natives;
    }
}
