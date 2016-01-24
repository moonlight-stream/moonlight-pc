package com.limelight.binding.video;


import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.nio.ByteBuffer;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.opengl.util.FPSAnimator;
import com.limelight.LimeLog;
import com.limelight.gui.RenderPanel;
import com.limelight.gui.StreamFrame;
import com.limelight.nvstream.av.video.VideoDepacketizer;
import com.limelight.nvstream.av.video.cpu.AvcDecoder;


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
	private ByteBuffer directBufferRGB;
	private float viewportX, viewportY;
    private boolean keepAspectRatio;

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
        final RenderPanel renderingSurface = frame.getRenderingSurface();
        
        keepAspectRatio = frame.getUserPreferences().isKeepAspectRatio();

        directBufferRGB = ByteBuffer.allocateDirect(4 * width * height);
        
        frame.addComponentListener(new ComponentListener() {
			@Override
			public void componentHidden(ComponentEvent arg0) {}
			@Override
			public void componentMoved(ComponentEvent arg0) {}
			@Override
			public void componentResized(ComponentEvent arg0) {
				glcanvas.setSize(renderingSurface.getSize());
			}
			@Override
			public void componentShown(ComponentEvent arg0) {}
        });

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
        animator.setUpdateFPSFrames(targetFps, System.out);
        return true;
    }

    
    public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width, int height) {
        GL2 gl = glautodrawable.getGL().getGL2();

        viewportX = width;
        viewportY = height;
        gl.glViewport(x, y, width, height);
    }

    public void init(GLAutoDrawable glautodrawable) {
        GL2 gl = glautodrawable.getGL().getGL2();
    	
        gl.glDisable(GL2.GL_DITHER);
        gl.glDisable(GL2.GL_MULTISAMPLE);
        gl.glPixelTransferi(GL2.GL_MAP_COLOR, GL2.GL_FALSE);
        gl.glPixelTransferi(GL2.GL_RED_SCALE, 1);
        gl.glPixelTransferi(GL2.GL_RED_BIAS, 0);
        gl.glPixelTransferi(GL2.GL_GREEN_SCALE, 1);
        gl.glPixelTransferi(GL2.GL_GREEN_BIAS, 0);
        gl.glPixelTransferi(GL2.GL_BLUE_SCALE, 1);
        gl.glPixelTransferi(GL2.GL_BLUE_BIAS, 0);
        gl.glPixelTransferi(GL2.GL_ALPHA_SCALE, 1);
        gl.glPixelTransferi(GL2.GL_ALPHA_BIAS, 0);
    }
    
    public void dispose(GLAutoDrawable glautodrawable) {
    }

	public void display(GLAutoDrawable glautodrawable) {
        GL2 gl = glautodrawable.getGL().getGL2();

        AvcDecoder.getRgbFrameBuffer(directBufferRGB, directBufferRGB.capacity());        
        
        gl.glClearColor(0, 0, 0, 0);

        float zoomX = viewportX / width;
        float zoomY = viewportY / height;
        
        if (keepAspectRatio) {
        	zoomX = zoomY = Math.min(zoomX, zoomY);
        }
        // centered
        gl.glRasterPos2f(-zoomX*(width/viewportX), zoomY*(height/viewportY));
		gl.glPixelZoom(zoomX, -zoomY);
        gl.glDrawPixels(width, height, GL2.GL_BGRA, GL2.GL_UNSIGNED_BYTE, directBufferRGB);
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

