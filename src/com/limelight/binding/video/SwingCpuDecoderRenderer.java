package com.limelight.binding.video;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;

import javax.swing.JFrame;

import com.limelight.LimeLog;
import com.limelight.nvstream.av.ByteBufferDescriptor;
import com.limelight.nvstream.av.DecodeUnit;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.av.video.VideoDepacketizer;
import com.limelight.nvstream.av.video.cpu.AvcDecoder;

/**
 * Implementation of a video decoder and renderer.
 * @author Cameron Gutman
 */
public class SwingCpuDecoderRenderer implements VideoDecoderRenderer {

	private Thread rendererThread;
	private int targetFps;
	private int width, height;

	private JFrame frame;
	private BufferedImage image;
	private boolean dying;
	
	private static final int DECODER_BUFFER_SIZE = 92*1024;
	private ByteBuffer decoderBuffer;
	
	// Only sleep if the difference is above this value
	private static final int WAIT_CEILING_MS = 8;
	
	private static final int REFERENCE_PIXEL = 0x01020304;
	
	/**
	 * Sets up the decoder and renderer to render video at the specified dimensions
	 * @param width the width of the video to render
	 * @param height the height of the video to render
	 * @param renderTarget what to render the video onto
	 * @param drFlags flags for the decoder and renderer
	 */
	public void setup(int width, int height, int redrawRate, Object renderTarget, int drFlags) {
		this.targetFps = redrawRate;
		this.width = width;
		this.height = height;
		
		// We only use one thread because each additional thread adds a frame of latency
		int avcFlags = AvcDecoder.BILINEAR_FILTERING | AvcDecoder.LOW_LATENCY_DECODE;
		int threadCount = 1;
		
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
				avcFlags |= AvcDecoder.NATIVE_COLOR_ARGB;
				image = optimalImage;
			}
			else if (redIndex == 1 && greenIndex == 2 && blueIndex == 3 && alphaIndex == 4) {
				LimeLog.info("Using optimal color space (RGBA)");
				avcFlags |= AvcDecoder.NATIVE_COLOR_RGBA;
				image = optimalImage;
			}
		}
		else {
			if (redIndex == 1 && greenIndex == 2 && blueIndex == 3) {
				LimeLog.info("Using optimal color space (RGB0)");
				avcFlags |= AvcDecoder.NATIVE_COLOR_RGB0;
				image = optimalImage;
			}
			else if (redIndex == 2 && greenIndex == 3 && blueIndex == 4) {
				LimeLog.info("Using optimal color space (0RGB)");
				avcFlags |= AvcDecoder.NATIVE_COLOR_0RGB;
				image = optimalImage;
			}
		}

		int err = AvcDecoder.init(width, height, avcFlags, threadCount);
		if (err != 0) {
			throw new IllegalStateException("AVC decoder initialization failure: "+err);
		}
		
		frame = (JFrame)renderTarget;

		if (image == null) {
			// The decoder renders to an RGB color model by default
			image = new BufferedImage(width, height,
	            BufferedImage.TYPE_INT_RGB);
		}
		
		decoderBuffer = ByteBuffer.allocate(DECODER_BUFFER_SIZE + AvcDecoder.getInputPaddingSize());
		
		LimeLog.info("Using software decoding");
	}

	/**
	 * Starts the decoding and rendering of the video stream on a new thread
	 */
	public void start(final VideoDepacketizer depacketizer) {
		rendererThread = new Thread() {
			@Override
			public void run() {
				long nextFrameTime = System.currentTimeMillis();
				int[] imageBuffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
				
				frame.createBufferStrategy(2);
				BufferStrategy strategy = frame.getBufferStrategy();
				
				if (strategy.getCapabilities().isPageFlipping()) {
					LimeLog.info("Using page flipping for buffer swaps");
				}
				else {
					LimeLog.info("Using blitting for buffer swaps");
				}
				
				LimeLog.info("Front buffer accelerated? "+strategy.getCapabilities().getFrontBufferCapabilities().isAccelerated());
				LimeLog.info("Back buffer accelerated? "+strategy.getCapabilities().getBackBufferCapabilities().isAccelerated());
				
				DecodeUnit du;
				while (!isInterrupted() && !dying)
				{
					du = depacketizer.pollNextDecodeUnit();
					if (du != null) {
						submitDecodeUnit(du);
					}
					
					long diff = nextFrameTime - System.currentTimeMillis();

					if (diff > WAIT_CEILING_MS) {
						continue;
					}
					
					nextFrameTime = computePresentationTimeMs(targetFps);
					
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
						do {
							do {
								Graphics g = strategy.getDrawGraphics();
								// make any remaining space black
								g.setColor(Color.BLACK);
								g.fillRect(0, 0, dx1, frame.getHeight());
								g.fillRect(0, 0, frame.getWidth(), dy1);
								g.fillRect(0, dy1+newHeight, frame.getWidth(), frame.getHeight());
								g.fillRect(dx1+newWidth, 0, frame.getWidth(), frame.getHeight());
								
								// draw the frame
								g.drawImage(image, dx1, dy1, dx1+newWidth, dy1+newHeight, 0, 0, width, height, null);
								g.dispose();
							} while (strategy.contentsRestored());
							strategy.show();
						} while (strategy.contentsLost());
					}
				}
			}
		};
		rendererThread.setPriority(Thread.MAX_PRIORITY);
		rendererThread.setName("Video - Renderer (CPU)");
		rendererThread.start();
	}
	
	/*
	 * Computes the amount of time to display a certain frame
	 */
	private long computePresentationTimeMs(int frameRate) {
		return System.currentTimeMillis() + (1000 / frameRate);
	}

	/**
	 * Stops the decoding and rendering of the video stream.
	 */
	public void stop() {
		dying = true;
		rendererThread.interrupt();
		
		try {
			rendererThread.join();
		} catch (InterruptedException e) { }
	}

	/**
	 * Releases resources held by the decoder.
	 */
	public void release() {
		AvcDecoder.destroy();
	}

	/**
	 * Give a unit to be decoded to the decoder.
	 * @param decodeUnit the unit to be decoded
	 * @return true if the unit was decoded successfully, false otherwise
	 */
	public boolean submitDecodeUnit(DecodeUnit decodeUnit) {
		byte[] data;
		
		// Use the reserved decoder buffer if this decode unit will fit
		if (decodeUnit.getDataLength() <= DECODER_BUFFER_SIZE) {
			decoderBuffer.clear();
			
			for (ByteBufferDescriptor bbd : decodeUnit.getBufferList()) {
				decoderBuffer.put(bbd.data, bbd.offset, bbd.length);
			}
			
			data = decoderBuffer.array();
		}
		else {
			data = new byte[decodeUnit.getDataLength()+AvcDecoder.getInputPaddingSize()];
			
			int offset = 0;
			for (ByteBufferDescriptor bbd : decodeUnit.getBufferList()) {
				System.arraycopy(bbd.data, bbd.offset, data, offset, bbd.length);
				offset += bbd.length;
			}
		}
		
		return (AvcDecoder.decode(data, 0, decodeUnit.getDataLength()) == 0);
	}

	public int getCapabilities() {
		return 0;
	}
}
