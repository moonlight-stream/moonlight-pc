package com.limelight.binding.video.debug;

import com.limelight.binding.video.SwingCpuDecoderRenderer;
import com.limelight.nvstream.av.video.cpu.AvcDecoder;

/**
 * Author: spartango
 * Date: 2/1/14
 * Time: 6:26 PM.
 */
public class DebugCpuDecoderRenderer extends SwingCpuDecoderRenderer {
    private long lastFrameTime;

    @Override protected void renderFrame(int[] imageBuffer) {
        // Render the frame normally
        double widthScale = (double) frame.getWidth() / width;
        double heightScale = (double) frame.getHeight() / height;
        double lowerScale = Math.min(widthScale, heightScale);
        int newWidth = (int) (width * lowerScale);
        int newHeight = (int) (height * lowerScale);

        int dx1 = 0;
        int dy1 = 0;
        if (frame.getWidth() > newWidth) {
            dx1 = (frame.getWidth() - newWidth) / 2;
        }
        if (frame.getHeight() > newHeight) {
            dy1 = (frame.getHeight() - newHeight) / 2;
        }

        // Time the decode
        long preDecode = System.currentTimeMillis();
        boolean decode = AvcDecoder.getRgbFrameInt(imageBuffer, imageBuffer.length);
        long decodeTime = System.currentTimeMillis() - preDecode;

        // Time the render
        long preRender = System.currentTimeMillis();
        if (decode) {
            graphics.drawImage(image, dx1, dy1, dx1 + newWidth, dy1 + newHeight, 0, 0, width, height, null);
        }
        long renderTime = System.currentTimeMillis() - preRender;

        // Render overlay
        renderOverlay(decodeTime, renderTime);

        // Save the time
        lastFrameTime = System.currentTimeMillis();
    }

    private void renderOverlay(long decodeTime, long renderTime) {
        long frameDelta = System.currentTimeMillis() - lastFrameTime;
        double frameRate = 1000.0 / frameDelta;
        // graphics.setColor(Color.white);
        String overlayInfo = decodeTime
                             + "ms + "
                             + renderTime
                             + "ms | "
                             + frameRate
                             + " / "
                             + targetFps
                             + " fps";
        System.out.println(overlayInfo);
        //graphics.drawString(overlayInfo, 20, 20);
    }
}
