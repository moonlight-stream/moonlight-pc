package com.limelight.binding.video;


import com.jogamp.opengl.util.FPSAnimator;
import com.limelight.LimeLog;
import com.limelight.gui.StreamFrame;
import com.limelight.nvstream.av.video.VideoDepacketizer;
import com.limelight.nvstream.av.video.cpu.AvcDecoder;

import javax.media.opengl.*;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.*;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.nio.IntBuffer;


/**
 * Author: spartango
 * Date: 2/1/14
 * Time: 11:42 PM.
 */
public class GLDecoderRenderer extends AbstractCpuDecoder implements GLEventListener {
	private final GLProfile glprofile;
	private final GLCapabilities glcapabilities;
	private final GLCanvas glcanvas;
	private FPSAnimator animator;
	private IntBuffer bufferRGB;
	private int[] imageBuffer;

    public GLDecoderRenderer() {
        GLProfile.initSingleton();
        glprofile = GLProfile.getDefault();
        glcapabilities = new GLCapabilities(glprofile);
        glcanvas = new GLCanvas(glcapabilities);
    }
    
    @Override
    public int getColorMode() {
        // Force the renderer to use a buffered image that's friendly with OpenGL
    	return AvcDecoder.NATIVE_COLOR_0RGB;
    }

    @Override
    public boolean setupInternal(Object renderTarget, int drFlags) {
        final StreamFrame frame = (StreamFrame) renderTarget;
        final JPanel renderingSurface = frame.getRenderingSurface();

        imageBuffer = new int[width * height];
        bufferRGB = IntBuffer.wrap(imageBuffer);

        glcanvas.setSize(renderingSurface.getSize());
        glcanvas.addGLEventListener(this);

        for (MouseListener m : renderingSurface.getMouseListeners()) {
            glcanvas.addMouseListener(m);
        }

        for (KeyListener k : renderingSurface.getKeyListeners()) {
            glcanvas.addKeyListener(k);
        }
        
        for (MouseWheelListener w : renderingSurface.getMouseWheelListeners()) {
            glcanvas.addMouseWheelListener(w);
        }

        for (MouseMotionListener m : renderingSurface.getMouseMotionListeners()) {
            glcanvas.addMouseMotionListener(m);
        }
        
        frame.setLayout(null);
        frame.add(glcanvas, 0, 0);
        glcanvas.setCursor(frame.getCursor());

        animator = new FPSAnimator(glcanvas, targetFps);
        
        LimeLog.info("Using OpenGL rendering");
        
        return true;
    }

    public boolean start(VideoDepacketizer depacketizer) {
    	if (!super.start(depacketizer)) {
    		return false;
    	}
        animator.start();
        return true;
    }

    public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width, int height) {
    }

    public void init(GLAutoDrawable glautodrawable) {
        GL2 gl = glautodrawable.getGL().getGL2();
    	
        gl.glDisable(GL2.GL_ALPHA_TEST);
        gl.glDisable(GL2.GL_BLEND);
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glDisable(GL2.GL_DITHER);
        gl.glDisable(GL2.GL_FOG);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_LOGIC_OP);
        gl.glDisable(GL2.GL_STENCIL_TEST);
        gl.glDisable(GL2.GL_TEXTURE_1D);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glPixelTransferi(GL2.GL_MAP_COLOR, GL2.GL_FALSE);
        gl.glPixelTransferi(GL2.GL_RED_SCALE, 1);
        gl.glPixelTransferi(GL2.GL_RED_BIAS, 0);
        gl.glPixelTransferi(GL2.GL_GREEN_SCALE, 1);
        gl.glPixelTransferi(GL2.GL_GREEN_BIAS, 0);
        gl.glPixelTransferi(GL2.GL_BLUE_SCALE, 1);
        gl.glPixelTransferi(GL2.GL_BLUE_BIAS, 0);
        gl.glPixelTransferi(GL2.GL_ALPHA_SCALE, 1);
        gl.glPixelTransferi(GL2.GL_ALPHA_BIAS, 0);
        gl.glRasterPos2f(-1, 1);
        gl.glPixelZoom(1, -1);
    }

    public void dispose(GLAutoDrawable glautodrawable) {
    }

    public void display(GLAutoDrawable glautodrawable) {
        GL2 gl = glautodrawable.getGL().getGL2();

        // Get an updated image if available
        // If not, the image buffer will be left unchanged
        AvcDecoder.getRgbFrameInt(imageBuffer, imageBuffer.length);
        
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glDrawPixels(width, height, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, bufferRGB);
    }

    /**
     * Stops the decoding and rendering of the video stream.
     */
    @Override
    public void stop() {
    	super.stop();
        animator.stop();
    }
}

