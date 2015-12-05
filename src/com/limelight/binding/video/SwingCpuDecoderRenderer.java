package com.limelight.binding.video;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;

import com.limelight.LimeLog;
import com.limelight.gui.RenderPanel;
import com.limelight.gui.StreamFrame;
import com.limelight.nvstream.av.video.VideoDepacketizer;
import com.limelight.nvstream.av.video.cpu.AvcDecoder;

/**
 * Implementation of a video decoder and renderer.
 * @author Cameron Gutman
 */
public class SwingCpuDecoderRenderer extends AbstractCpuDecoder implements RenderPanel.RenderPainter {

	private RenderPanel renderPanel;
	
	private BufferedImage image;
	private int[] imageBuffer;
	private int colorMode;
	
	private static final int REFERENCE_PIXEL = 0x01020304;
	
	@Override
	public int getColorMode() {
		if (image == null) {
			GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment.
					getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
			
			// Attempt to use an optimized buffered image to avoid an additional
			// color space conversion on each redraw.
			BufferedImage optimalImage = graphicsConfiguration.createCompatibleImage(width, height, Transparency.OPAQUE);
			ColorModel optimalCm = optimalImage.getColorModel();
			int redIndex = optimalCm.getRed(REFERENCE_PIXEL);
			int greenIndex = optimalCm.getGreen(REFERENCE_PIXEL);
			int blueIndex = optimalCm.getBlue(REFERENCE_PIXEL);
			if (optimalCm.hasAlpha()) {
				int alphaIndex = optimalCm.getAlpha(REFERENCE_PIXEL);
				if (alphaIndex == 1 && redIndex == 2 && greenIndex == 3 && blueIndex == 4) {
					LimeLog.info("Using optimal color space (ARGB)");
					colorMode = AvcDecoder.NATIVE_COLOR_ARGB;
					image = optimalImage;
				}
				else if (redIndex == 1 && greenIndex == 2 && blueIndex == 3 && alphaIndex == 4) {
					LimeLog.info("Using optimal color space (RGBA)");
					colorMode = AvcDecoder.NATIVE_COLOR_RGBA;
					image = optimalImage;
				}
			}
			else {
				if (redIndex == 1 && greenIndex == 2 && blueIndex == 3) {
					LimeLog.info("Using optimal color space (RGB0)");
					colorMode = AvcDecoder.NATIVE_COLOR_RGB0;
					image = optimalImage;
				}
				else if (redIndex == 2 && greenIndex == 3 && blueIndex == 4) {
					LimeLog.info("Using optimal color space (0RGB)");
					colorMode = AvcDecoder.NATIVE_COLOR_0RGB;
					image = optimalImage;
				}
			}
			
			if (image == null) {
				// The decoder renders to an RGB color model by default
				image = new BufferedImage(width, height,
		            BufferedImage.TYPE_INT_RGB);
			}
		}
		
		return colorMode;
	}

	@Override
	public boolean setupInternal(Object renderTarget, int drFlags) {
        StreamFrame frame = (StreamFrame) renderTarget;
        
        renderPanel = frame.getRenderingSurface();
		imageBuffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
		
		LimeLog.info("Using software rendering");
		
		return true;
	}

	/**
	 * Starts the decoding and rendering of the video stream on a new thread
	 */
	public boolean start(final VideoDepacketizer depacketizer) {
    	if (!super.start(depacketizer)) {
    		return false;
    	}
    	
    	renderPanel.setRenderPainter(this);
		
    	return true;
	}
	
	/**
	 * Stops the decoding and rendering of the video stream.
	 */
	@Override
	public void stop() {
		super.stop();
		
		// Stop receiving repaint callbacks
		renderPanel.setRenderPainter(null);
	}
	
	@Override
	protected void onFrameDecoded() {
		renderPanel.repaint();
	}

	@Override
	public void paintPanel(Graphics g) {
		// Calculate the scaling factor
		double widthScale = (double)renderPanel.getWidth() / width;
		double heightScale = (double)renderPanel.getHeight() / height;
		double lowerScale = Math.min(widthScale, heightScale);
		int newWidth = (int)(width * lowerScale);
		int newHeight = (int)(height * lowerScale);

		// Perform the scaling
		int dx1 = 0;
		int dy1 = 0;
		if (renderPanel.getWidth() > newWidth) {
			dx1 = (renderPanel.getWidth() - newWidth)/2;
		}
		if (renderPanel.getHeight() > newHeight) {
			dy1 = (renderPanel.getHeight() - newHeight)/2;
		}
		
		// Fetch a new image. If one isn't ready, the image buffer will
		// remain the same and be painted again.
		AvcDecoder.getRgbFrameInt(imageBuffer, imageBuffer.length);
		
		// make any remaining space black
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, renderPanel.getWidth(), renderPanel.getHeight());
		
		// draw the frame
		g.drawImage(image, dx1, dy1, dx1+newWidth, dy1+newHeight, 0, 0, width, height, null);
		
		g.dispose();
	}
}
