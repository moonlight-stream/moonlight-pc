package com.limelight.binding.video;


import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.limelight.nvstream.av.ByteBufferDescriptor;
import com.limelight.nvstream.av.DecodeUnit;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.av.video.cpu.AvcDecoder;

import javax.media.opengl.*;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;


/**
 * Author: spartango
 * Date: 2/1/14
 * Time: 11:42 PM.
 */
public class GLDecoderRenderer implements VideoDecoderRenderer, GLEventListener {
    protected int targetFps;
    protected int width, height;

    protected Graphics      graphics;
    protected JFrame        frame;
    protected BufferedImage image;
    protected int[]         imageBuffer;

    protected static final int DECODER_BUFFER_SIZE = 92 * 1024;
    protected ByteBuffer decoderBuffer;

    private long lastRender = System.currentTimeMillis();

    private final GLProfile      glprofile;
    private final GLCapabilities glcapabilities;
    private final GLCanvas       glcanvas;
    private       Animator       animator;

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

        frame = (JFrame) renderTarget;
        graphics = frame.getGraphics();

        // Force the renderer to use a buffered image that's friendly with OpenGL
        avcFlags |= AvcDecoder.NATIVE_COLOR_ARGB;
        image = new BufferedImage(width, height,
                                  BufferedImage.TYPE_INT_ARGB);
        imageBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        int err = AvcDecoder.init(width, height, avcFlags, threadCount);
        if (err != 0) {
            throw new IllegalStateException("AVC decoder initialization failure: " + err);
        }

        decoderBuffer = ByteBuffer.allocate(DECODER_BUFFER_SIZE + AvcDecoder.getInputPaddingSize());
        System.out.println("Using software decoding");

        // Add canvas to the frame
        glcanvas.setSize(width, height);
        glcanvas.addGLEventListener(this);

        for (MouseListener m : frame.getMouseListeners()) {
            glcanvas.addMouseListener(m);
        }

        for (KeyListener k : frame.getKeyListeners()) {
            glcanvas.addKeyListener(k);
        }

        for (MouseMotionListener m : frame.getMouseMotionListeners()) {
            glcanvas.addMouseMotionListener(m);
        }

        frame.setLayout(null);
        frame.add(glcanvas, 0, 0);

        animator = new Animator(glcanvas);
    }

    @Override public void start() {
        animator.start();
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
        long decodeStart = System.currentTimeMillis();
        // Render the frame into the buffered image
        boolean decoded = AvcDecoder.getRgbFrameInt(imageBuffer, imageBuffer.length);
        long decodeTime = System.currentTimeMillis() - decodeStart;

        long renderStart = System.currentTimeMillis();

        GL2 gl = glautodrawable.getGL().getGL2();
        int width = glautodrawable.getWidth();
        int height = glautodrawable.getHeight();

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

//        BufferedImage compatible = new BufferedImage(imageWidth, imageHeight, image.getType());
//        Graphics2D g = compatible.createGraphics();
//        AffineTransform gt = new AffineTransform();
//        gt.translate(0, imageHeight);
//        gt.scale(1, -1d);
//        g.transform(gt);
//        g.drawImage(image, null, null);
//        g.dispose();

        DataBufferInt buffer =
                (DataBufferInt) image.getRaster().getDataBuffer();
        IntBuffer bufferRGB = IntBuffer.wrap(buffer.getData());

        gl.glEnable(gl.GL_TEXTURE_2D);
        Texture texture = new Texture(gl,
                                      new TextureData(glprofile,
                                                      4,
                                                      imageWidth,
                                                      imageHeight,
                                                      0,
                                                      gl.GL_BGRA,
                                                      gl.GL_UNSIGNED_INT_8_8_8_8_REV,
                                                      false,
                                                      false,
                                                      true,
                                                      bufferRGB,
                                                      null));
        texture.enable(gl);
        texture.bind(gl);

        gl.glBegin(gl.GL_QUADS);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-1.0f, 1.0f, 1.0f); // Bottom Left Of The Texture and Quad

        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(1.0f, 1.0f, 1.0f); // Bottom Right Of The Texture and Quad

        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(1.0f, -1.0f, 1.0f); // Top Right Of The Texture and Quad

        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-1.0f, -1.0f, 1.0f);

        gl.glEnd();
        texture.disable(gl);
        texture.destroy(gl);

//        gl.glTexImage2D(gl.GL_TEXTURE_2D,
//                        0,
//                        4,
//                        imageWidth,
//                        imageHeight,
//                        0,
//                        gl.GL_BGRA,
//                        gl.GL_UNSIGNED_INT_8_8_8_8_REV,
//                        bufferRGB);


//        gl.glDrawPixels(imageWidth, imageHeight,
//                        gl.GL_BGRA, gl.GL_UNSIGNED_INT_8_8_8_8_REV,
//                        bufferRGB);

        long renderTime = System.currentTimeMillis() - renderStart;
        long refreshTime = System.currentTimeMillis() - lastRender;

        System.out.println("Render: "
                           + refreshTime
                           + "("
                           + decodeTime
                           + " + "
                           + renderTime
                           + ") ms | "
                           + (1000.0
                              / refreshTime)
                           + " fps");

        lastRender = System.currentTimeMillis();
    }

    /**
     * Releases resources held by the decoder.
     */
    @Override public void release() {
        AvcDecoder.destroy();
    }


    /**
     * Give a unit to be decoded to the decoder.
     *
     * @param decodeUnit the unit to be decoded
     * @return true if the unit was decoded successfully, false otherwise
     */
    @Override public boolean submitDecodeUnit(DecodeUnit decodeUnit) {
        byte[] data;

        // Use the reserved decoder buffer if this decode unit will fit
        if (decodeUnit.getDataLength() <= DECODER_BUFFER_SIZE) {
            decoderBuffer.clear();

            for (ByteBufferDescriptor bbd : decodeUnit.getBufferList()) {
                decoderBuffer.put(bbd.data, bbd.offset, bbd.length);
            }

            data = decoderBuffer.array();
        } else {
            data = new byte[decodeUnit.getDataLength() + AvcDecoder.getInputPaddingSize()];

            int offset = 0;
            for (ByteBufferDescriptor bbd : decodeUnit.getBufferList()) {
                System.arraycopy(bbd.data, bbd.offset, data, offset, bbd.length);
                offset += bbd.length;
            }
        }

        return (AvcDecoder.decode(data, 0, decodeUnit.getDataLength()) == 0);
    }

    /**
     * Stops the decoding and rendering of the video stream.
     */
    @Override public void stop() {
        animator.stop();
    }
}

