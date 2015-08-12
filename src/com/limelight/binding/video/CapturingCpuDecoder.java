package com.limelight.binding.video;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.limelight.nvstream.av.ByteBufferDescriptor;
import com.limelight.nvstream.av.DecodeUnit;

public abstract class CapturingCpuDecoder extends AbstractCpuDecoder {
	
	private FileOutputStream fout;
	
	public boolean setup(int width, int height, int redrawRate, Object renderTarget, int drFlags) {
		try {
			fout = new FileOutputStream("capture.h264");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		
		return super.setup(width, height, redrawRate, renderTarget, drFlags);
	}
	
	@Override
	public boolean submitDecodeUnit(DecodeUnit decodeUnit) {
		for (ByteBufferDescriptor bbd = decodeUnit.getBufferHead();
				bbd != null; bbd = bbd.nextDescriptor) {
			try {
				fout.write(bbd.data, bbd.offset, bbd.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return super.submitDecodeUnit(decodeUnit);
	}
	
	@Override
	public void stop() {
		super.stop();
		try {
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
