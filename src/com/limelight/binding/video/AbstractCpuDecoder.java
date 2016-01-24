package com.limelight.binding.video;

import java.nio.ByteBuffer;

import com.limelight.LimeLog;
import com.limelight.nvstream.av.ByteBufferDescriptor;
import com.limelight.nvstream.av.DecodeUnit;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.av.video.VideoDepacketizer;
import com.limelight.nvstream.av.video.cpu.AvcDecoder;

public abstract class AbstractCpuDecoder extends VideoDecoderRenderer {
	
	private static final int DECODER_BUFFER_SIZE = 256*1024;
	
	protected int width, height, targetFps;
	
	private Thread decoderThread;
	volatile protected boolean dying;
	
	private ByteBuffer decoderBuffer;
	
	private int totalFrames;
	private long totalDecoderTimeMs;
	private int inputPaddingSize;
	
	public abstract boolean setupInternal(Object renderTarget, int drFlags);
	
	public abstract int getColorMode();
	
	
	// VideoDecoderRenderer abstract method @Overrides
	/**
	 * Sets up the decoder and renderer to render video at the specified dimensions
	 * @param width the width of the video to render
	 * @param height the height of the video to render
	 * @param renderTarget what to render the video onto
	 * @param drFlags flags for the decoder and renderer
	 */
	public boolean setup(VideoFormat format, int width, int height, int redrawRate, Object renderTarget, int drFlags) {
		this.width = width;
		this.height = height;
		this.targetFps = redrawRate;
		this.inputPaddingSize = AvcDecoder.getInputPaddingSize();
		
		if (format != VideoFormat.H264) {
			return false;
		}
		
		decoderBuffer = ByteBuffer.allocateDirect(DECODER_BUFFER_SIZE + inputPaddingSize);

		int avcFlags = AvcDecoder.FAST_BILINEAR_FILTERING | getColorMode();
		int threadCount = 2;
		
		LimeLog.info("Using software decoding with thread count: "+threadCount);

		int err = AvcDecoder.init(width, height, avcFlags, threadCount);
		if (err != 0) {
			LimeLog.severe("AVC decoder initialization failure: "+err);
			return false;
		}
		
		return setupInternal(renderTarget, drFlags);
	}

	/**
	 * Starts the decoding and rendering of the video stream on a new thread
	 */
	public boolean start(final VideoDepacketizer depacketizer) {
		decoderThread = new Thread() {
			@Override
			public void run() {
				DecodeUnit du;
				while (!dying) {
					try {
						du = depacketizer.takeNextDecodeUnit();
					} catch (InterruptedException e1) {
						return;
					}
					
					if (du != null) {
						submitDecodeUnit(du);
						depacketizer.freeDecodeUnit(du);
					}
					
				}
			}
		};
		decoderThread.setPriority(Thread.MAX_PRIORITY - 1);
		decoderThread.setName("Video - Decoder (CPU)");
		decoderThread.start();
		return true;
	}
	
	/**
	 * Stops the decoding and rendering of the video stream.
	 */
	public void stop() {
		dying = true;
		decoderThread.interrupt();
		
		try {
			decoderThread.join();
		} catch (InterruptedException e) { }
	}

	/**
	 * Releases resources held by the decoder.
	 */
	public void release() {
		AvcDecoder.destroy();
	}
	// End of VideoDecoderRenderer @Overrides
	
	

	/**
	 * Give a unit to be decoded to the decoder.
	 * @param decodeUnit the unit to be decoded
	 * @return true if the unit was decoded successfully, false otherwise
	 */
	public boolean submitDecodeUnit(DecodeUnit decodeUnit) {
		
		if (decoderBuffer.capacity() < decodeUnit.getDataLength() + inputPaddingSize) {
			int newCapacity = (int)(1.15f * decodeUnit.getDataLength()) + inputPaddingSize;
			LimeLog.info("Reallocating decoder buffer from " + decoderBuffer.capacity() + " to " + newCapacity + " bytes");
			
			decoderBuffer = ByteBuffer.allocateDirect(newCapacity);
		}
		
		decoderBuffer.clear();
		
		for (ByteBufferDescriptor bbd = decodeUnit.getBufferHead();
				bbd != null; bbd = bbd.nextDescriptor) {
			decoderBuffer.put(bbd.data, bbd.offset, bbd.length);
		}
		
		boolean success = (AvcDecoder.decodeBuffer(decoderBuffer, decodeUnit.getDataLength()) == 0);
		
		if (success) {
			long timeAfterDecode = System.nanoTime() / 1000000L;
			
		    // Add delta time to the totals (excluding probable outliers)
		    long delta = timeAfterDecode - decodeUnit.getReceiveTimestamp();
			if (delta >= 0 && delta < 300) {
			    totalDecoderTimeMs += delta;
			    totalFrames++;
			}
		}
		
		return true;
	}

	public int getCapabilities() {
		return 0;
	}

	public int getAverageDecoderLatency() {
		if (totalFrames == 0) {
			return 0;
		}
		return (int)(totalDecoderTimeMs / totalFrames);
	}
}
