package com.limelight.binding.video;

import java.nio.ByteBuffer;

import com.limelight.LimeLog;
import com.limelight.nvstream.av.ByteBufferDescriptor;
import com.limelight.nvstream.av.DecodeUnit;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.av.video.VideoDepacketizer;
import com.limelight.nvstream.av.video.cpu.AvcDecoder;

public abstract class AbstractCpuDecoder extends VideoDecoderRenderer {
	private Thread decoderThread;
	protected int width, height, targetFps;
	protected boolean dying;
	
	private static final int DECODER_BUFFER_SIZE = 92*1024;
	private ByteBuffer decoderBuffer;
	
	private int totalFrames;
	private long totalDecoderTimeMs;
	
	public abstract boolean setupInternal(Object renderTarget, int drFlags);
	
	public abstract int getColorMode();
	
	/**
	 * Sets up the decoder and renderer to render video at the specified dimensions
	 * @param width the width of the video to render
	 * @param height the height of the video to render
	 * @param renderTarget what to render the video onto
	 * @param drFlags flags for the decoder and renderer
	 */
	public boolean setup(int width, int height, int redrawRate, Object renderTarget, int drFlags) {
		this.width = width;
		this.height = height;
		this.targetFps = redrawRate;
		
		decoderBuffer = ByteBuffer.allocate(DECODER_BUFFER_SIZE + AvcDecoder.getInputPaddingSize());
		LimeLog.info("Using software decoding");
		
		// Use 2 decoding threads
		int avcFlags = AvcDecoder.FAST_BILINEAR_FILTERING | getColorMode();
		int threadCount = 2;

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
			
			for (ByteBufferDescriptor bbd = decodeUnit.getBufferHead();
					bbd != null; bbd = bbd.nextDescriptor) {
				decoderBuffer.put(bbd.data, bbd.offset, bbd.length);
			}
			
			data = decoderBuffer.array();
		}
		else {
			data = new byte[decodeUnit.getDataLength()+AvcDecoder.getInputPaddingSize()];
			
			int offset = 0;
			for (ByteBufferDescriptor bbd = decodeUnit.getBufferHead();
					bbd != null; bbd = bbd.nextDescriptor) {
				System.arraycopy(bbd.data, bbd.offset, data, offset, bbd.length);
				offset += bbd.length;
			}
		}
		
		boolean success = (AvcDecoder.decode(data, 0, decodeUnit.getDataLength()) == 0);
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
