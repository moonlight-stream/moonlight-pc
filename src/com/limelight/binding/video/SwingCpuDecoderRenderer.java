package com.limelight.binding.video;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;

import javax.swing.JFrame;

import com.limelight.LimeLog;
import com.limelight.nvstream.av.video.VideoDepacketizer;
import com.limelight.nvstream.av.video.cpu.AvcDecoder;

/**
 * Implementation of a video decoder and renderer.
 * @author Cameron Gutman
 */
public class SwingCpuDecoderRenderer extends AbstractCpuDecoder {

	private Thread rendererThread;

	private JFrame frame;
	
	
	private BufferedImage image;
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
		frame = (JFrame)renderTarget;
		
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
		rendererThread = new Thread() {
			@Override
			public void run() {
				int[] imageBuffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
				
				while (!dying)
				{	
					int sides = frame.getInsets().left + frame.getInsets().right;
					int topBottom = frame.getInsets().top + frame.getInsets().bottom;
					
					double widthScale = (double)(frame.getWidth() - sides) / width;
					double heightScale = (double)(frame.getHeight() - topBottom) / height;
					double lowerScale = Math.min(widthScale, heightScale);
					int newWidth = (int)(width * lowerScale);
					int newHeight = (int)(height * lowerScale);
					
					int dx1 = 0;
					int dy1 = 0;
					if (frame.getWidth() > newWidth) {
						dx1 = (frame.getWidth() + frame.getInsets().left - newWidth)/2;
					}
					if (frame.getHeight() > newHeight) {
						dy1 = (frame.getHeight() + frame.getInsets().top - newHeight)/2;
					}
					
					if (AvcDecoder.getRgbFrameInt(imageBuffer, imageBuffer.length)) {
						
						Graphics g = frame.getGraphics();
						// make any remaining space black
						g.setColor(Color.BLACK);
						g.fillRect(0, 0, dx1, frame.getHeight());
						g.fillRect(0, 0, frame.getWidth(), dy1);
						g.fillRect(0, dy1+newHeight, frame.getWidth(), frame.getHeight());
						g.fillRect(dx1+newWidth, 0, frame.getWidth(), frame.getHeight());
						
						// draw the frame
						g.drawImage(image, dx1, dy1, dx1+newWidth, dy1+newHeight, 0, 0, width, height, null);
						g.dispose();
					}
					else {
						// Wait and try again soon
						try {
							Thread.sleep(5);
						} catch (InterruptedException e) {
							break;
						}
					}
				}
			}
		};
		rendererThread.setPriority(Thread.MAX_PRIORITY);
		rendererThread.setName("Video - Renderer (CPU)");
		rendererThread.start();
		return true;
	}
	
	
	
	/**
	 * Stops the decoding and rendering of the video stream.
	 */
	@Override
	public void stop() {
		super.stop();
		
		rendererThread.interrupt();
		
		try {
			rendererThread.join();
		} catch (InterruptedException e) { }
	}
}
