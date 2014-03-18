package com.limelight.binding.video;

import com.limelight.nvstream.av.video.cpu.AvcDecoder;

import javax.swing.*;
import java.awt.*;

/**
 * Author: spartango
 * Date: 2/1/14
 * Time: 9:44 PM.
 */
public class UnifiedDecoderRenderer extends SwingCpuDecoderRenderer {

    private long lastDraw = System.currentTimeMillis();
    private JComponent component;

    @Override public void setup(int width, int height, int redrawRate, Object renderTarget, int drFlags) {
        super.setup(width, height, redrawRate, renderTarget, drFlags);

        // Add a Jcomponent to the frame
        component = new JComponent() {
			private static final long serialVersionUID = 1L;

			@Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };
        component.setSize(width, height);
        frame.setLayout(null);
        frame.add(component, 0, 0);
    }

    @Override protected void renderFrame(int[] imageBuffer) {
        //long decodeStart = System.currentTimeMillis();
        // Render the frame into the buffered image
        boolean decoded = AvcDecoder.getRgbFrameInt(imageBuffer, imageBuffer.length);
        // long decodeTime = System.currentTimeMillis() - decodeStart;

        // Request a repaint
        if (decoded) {
            component.repaint();
        }
    }

    protected void draw(Graphics g) {
        long renderStart = System.currentTimeMillis();
        // Scaling for the window
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
        g.drawImage(image, dx1, dy1, dx1 + newWidth, dy1 + newHeight, 0, 0, width, height, null);

        long renderTime = System.currentTimeMillis() - renderStart;
        long refreshTime = System.currentTimeMillis() - lastDraw;
        System.out.println(refreshTime + "(" + renderTime + ")" + "ms | " + (1000.0 / renderTime) + "fps");
        lastDraw = System.currentTimeMillis();
    }

}
