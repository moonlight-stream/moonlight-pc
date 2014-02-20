package com.limelight.binding.video;

import com.limelight.nvstream.av.ByteBufferDescriptor;
import com.limelight.nvstream.av.DecodeUnit;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.av.video.cpu.AvcDecoder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;

/**
 * Implementation of a video decoder and renderer.
 * @author Cameron Gutman
 */
public class SwingCpuDecoderRenderer implements VideoDecoderRenderer {

	private Thread rendererThread;
	protected int targetFps;
	protected int width, height;

	protected Graphics graphics;
	protected JFrame frame;
	protected BufferedImage image;

	protected static final int DECODER_BUFFER_SIZE = 92*1024;
	protected ByteBuffer decoderBuffer;

	// Only sleep if the difference is above this value
	public static final int WAIT_CEILING_MS = 8;

	private static final int REFERENCE_PIXEL = 0x01020304;

	/**
	 * Sets up the decoder and renderer to render video at the specified dimensions
	 * @param width the width of the video to render
	 * @param height the height of the video to render
	 * @param renderTarget what to render the video onto
	 * @param drFlags flags for the decoder and renderer
	 */
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
		BufferedImage optimalImage = graphicsConfiguration.createCompatibleImage(width, height, Transparency.OPAQUE);
		ColorModel optimalCm = optimalImage.getColorModel();
		int redIndex = optimalCm.getRed(REFERENCE_PIXEL);
		int greenIndex = optimalCm.getGreen(REFERENCE_PIXEL);
		int blueIndex = optimalCm.getBlue(REFERENCE_PIXEL);
		if (optimalCm.hasAlpha()) {
			int alphaIndex = optimalCm.getAlpha(REFERENCE_PIXEL);
			if (alphaIndex == 1 && redIndex == 2 && greenIndex == 3 && blueIndex == 4) {
				System.out.println("Using optimal color space (ARGB)");
				avcFlags |= AvcDecoder.NATIVE_COLOR_ARGB;
				image = optimalImage;
			}
			else if (redIndex == 1 && greenIndex == 2 && blueIndex == 3 && alphaIndex == 4) {
				System.out.println("Using optimal color space (RGBA)");
				avcFlags |= AvcDecoder.NATIVE_COLOR_RGBA;
				image = optimalImage;
			}
		}
		else {
			if (redIndex == 1 && greenIndex == 2 && blueIndex == 3) {
				System.out.println("Using optimal color space (RGB0)");
				avcFlags |= AvcDecoder.NATIVE_COLOR_RGB0;
				image = optimalImage;
			}
			else if (redIndex == 2 && greenIndex == 3 && blueIndex == 4) {
				System.out.println("Using optimal color space (0RGB)");
				avcFlags |= AvcDecoder.NATIVE_COLOR_0RGB;
				image = optimalImage;
			}
		}

		int err = AvcDecoder.init(width, height, avcFlags, threadCount);
		if (err != 0) {
			throw new IllegalStateException("AVC decoder initialization failure: "+err);
		}

		frame = (JFrame)renderTarget;
		graphics = frame.getGraphics();

        if (image == null) {
			// The decoder renders to an RGB color model by default
			image = new BufferedImage(width, height,
	            BufferedImage.TYPE_INT_RGB);
		}

		decoderBuffer = ByteBuffer.allocate(DECODER_BUFFER_SIZE + AvcDecoder.getInputPaddingSize());
		System.out.println("Using software decoding");
	}

	/**
	 * Starts the decoding and rendering of the video stream on a new thread
	 */
	@Override public void start() {
		rendererThread = new Thread() {
			@Override
			public void run() {
				long nextFrameTime = System.currentTimeMillis();
				int[] imageBuffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();

				while (!isInterrupted())
				{
					try {
                        delayFrame(nextFrameTime);
                        nextFrameTime = computePresentationTimeMs(targetFps);
                    } catch (InterruptedException e) {
						return;
					}

                    renderFrame(imageBuffer);
				}
			}
		};
		rendererThread.setName("Video - Renderer (CPU)");
		rendererThread.start();
	}

    protected void delayFrame(final long nextFrameTime) throws InterruptedException {
        // Time the frame to match framerate
        long diff = nextFrameTime - System.currentTimeMillis();
        if (diff < WAIT_CEILING_MS) {
            // We must call Thread.sleep in order to be interruptible
            diff = 0;
        }

        Thread.sleep(diff);
    }

    protected void renderFrame(final int[] imageBuffer) {
        // Scaling for the window
        double widthScale = (double)frame.getWidth() / width;
        double heightScale = (double)frame.getHeight() / height;
        double lowerScale = Math.min(widthScale, heightScale);
        int newWidth = (int)(width * lowerScale);
        int newHeight = (int)(height * lowerScale);

        int dx1 = 0;
        int dy1 = 0;
        if (frame.getWidth() > newWidth) {
            dx1 = (frame.getWidth()-newWidth)/2;
        }
        if (frame.getHeight() > newHeight) {
            dy1 = (frame.getHeight()-newHeight)/2;
        }

        if (AvcDecoder.getRgbFrameInt(imageBuffer, imageBuffer.length)) {
            graphics.drawImage(image, dx1, dy1, dx1+newWidth, dy1+newHeight, 0, 0, width, height, null);
        }
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
	@Override public void stop() {
		rendererThread.interrupt();

		try {
			rendererThread.join();
		} catch (InterruptedException e) { }
	}

	/**
	 * Releases resources held by the decoder.
	 */
	@Override public void release() {
		AvcDecoder.destroy();
	}

	/**
	 * Give a unit to be decoded to the decoder.
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
