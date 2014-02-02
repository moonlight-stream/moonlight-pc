package com.limelight.binding.video;


import com.limelight.nvstream.av.video.cpu.AvcDecoder;

import javax.media.opengl.*;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;


/**
 * Author: spartango
 * Date: 2/1/14
 * Time: 11:42 PM.
 */
public class GLDecoderRenderer extends SwingCpuDecoderRenderer implements GLEventListener {

    private final GLProfile      glprofile;
    private final GLCapabilities glcapabilities;
    private final GLCanvas       glcanvas;

    public GLDecoderRenderer() {
        GLProfile.initSingleton();
        glprofile = GLProfile.getDefault();
        glcapabilities = new GLCapabilities(glprofile);
        glcanvas = new GLCanvas(glcapabilities);
    }

    @Override public void setup(int width, int height, int redrawRate, Object renderTarget, int drFlags) {
        this.targetFps = redrawRate;
        this.width = width;
        this.height = height;

        // Two threads to ease the work, especially for higher resolutions and frame rates
        int avcFlags = AvcDecoder.BILINEAR_FILTERING;
        int threadCount = 2;

        GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment.
                getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        // Attempt to use an optimized buffered image to avoid an additional
        // color space conversion on each redraw.

        frame = (JFrame)renderTarget;
        graphics = frame.getGraphics();


        // Force the renderer
        avcFlags |= AvcDecoder.NATIVE_COLOR_ARGB;
        image = new BufferedImage(width, height,
                                  BufferedImage.TYPE_INT_ARGB);

        int err = AvcDecoder.init(width, height, avcFlags, threadCount);
        if (err != 0) {
            throw new IllegalStateException("AVC decoder initialization failure: "+err);
        }

        decoderBuffer = ByteBuffer.allocate(DECODER_BUFFER_SIZE + AvcDecoder.getInputPaddingSize());
        System.out.println("Using software decoding");


        // Add canvas to the frame
        glcanvas.setSize(width, height);
        glcanvas.addGLEventListener(this);

        frame.setLayout(null);
        frame.add(glcanvas, 0, 0);
    }


    @Override protected void renderFrame(int[] imageBuffer) {
        //long decodeStart = System.currentTimeMillis();
        // Render the frame into the buffered image
        boolean decoded = AvcDecoder.getRgbFrameInt(imageBuffer, imageBuffer.length);
        // long decodeTime = System.currentTimeMillis() - decodeStart;

        // Request a repaint
        if (decoded) {
            // Canvas draw
            glcanvas.repaint();
        }
    }

    @Override
    public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width, int height) {
    }

    @Override
    public void init(GLAutoDrawable glautodrawable) {
    }

    @Override
    public void dispose(GLAutoDrawable glautodrawable) {
    }

    @Override
    public void display(GLAutoDrawable glautodrawable) {
        long renderStart = System.currentTimeMillis();

        GL2 gl = glautodrawable.getGL().getGL2();
        int width = glautodrawable.getWidth();
        int height = glautodrawable.getHeight();

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

//        WritableRaster raster =
//                Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
//                                               imageWidth,
//                                               imageHeight,
//                                               4,
//                                               null);
//        ComponentColorModel colorModel =
//                new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
//                                        new int[]{8, 8, 8, 8},
//                                        true,
//                                        false,
//                                        ComponentColorModel.OPAQUE,
//                                        DataBuffer.TYPE_BYTE);

        BufferedImage compatible = new BufferedImage(imageWidth, imageHeight, image.getType());

        Graphics2D g = compatible.createGraphics();
        AffineTransform gt = new AffineTransform();
        gt.translate(0, imageHeight);
        gt.scale(1, -1d);
        g.transform(gt);
        g.drawImage(image, null, null);
        g.dispose();

        DataBufferInt buffer =
                (DataBufferInt) compatible.getRaster().getDataBuffer();
        IntBuffer bufferRGB = IntBuffer.wrap(buffer.getData());

        gl.glDrawPixels(imageWidth, imageHeight,
                        gl.GL_BGRA, gl.GL_UNSIGNED_INT_8_8_8_8_REV,
                        bufferRGB);

        long renderTime = System.currentTimeMillis() - renderStart;
        // graphics.setColor(Color.white);
        System.out.println("Render: " + renderTime + "ms");
    }

}

