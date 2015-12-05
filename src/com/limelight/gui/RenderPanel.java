package com.limelight.gui;

import java.awt.Graphics;

import javax.swing.JPanel;

public class RenderPanel extends JPanel {
	private static final long serialVersionUID = -821718264220699369L;
	
	private RenderPainter renderPainter;
	
	public void setRenderPainter(RenderPainter painter) {
		this.renderPainter = painter;
	}
	
	@Override
    protected void paintComponent(Graphics g) {
		if (renderPainter == null) {
			super.paintComponent(g);
			return;
		}
		
		renderPainter.paintPanel(g);
    }
	
	public interface RenderPainter {
		public void paintPanel(Graphics g);
	}
}