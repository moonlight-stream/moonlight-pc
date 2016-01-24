 package com.limelight.gui;

import com.limelight.settings.PreferencesManager;
import com.limelight.settings.PreferencesManager.Preferences;
import com.limelight.settings.PreferencesManager.Preferences.Resolution;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A frame that holds user preferences such as streaming resolution
 * @author Diego Waxemberg
 */
public class PreferencesFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private JComboBox<Resolution> resolution;
	private JLabel bitrateLabel;
	private JSlider bitrate;
	private JCheckBox fullscreen, allowResolutionChange, keepAspectRatio, localAudio;
	
	/**
	 * Construcs a new frame and loads the saved preferences.
	 * <br>The frame is not made visible until a call to <br>build()</br> is made.
	 */
	public PreferencesFrame() {
		super("Preferences");
		this.setSize(350, 340);
		this.setResizable(false);
	}
	
	/**
	 * Constructs all components of the frame and makes the frame visible to the user.
	 */
	public void build() {
		final Preferences prefs = PreferencesManager.getPreferences();
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		resolution = new JComboBox<Resolution>();
		for (Resolution res : Resolution.values()) {
			resolution.addItem(res);
		}
		
		resolution.setSelectedItem(prefs.getResolution());
		
		bitrateLabel = new JLabel("Maximum Bitrate = " + prefs.getBitrate() + " Mbps", JLabel.CENTER);
		bitrate = new JSlider(JSlider.HORIZONTAL, 0, 100, prefs.getBitrate());
		bitrate.setMajorTickSpacing(20);
		bitrate.setMinorTickSpacing(1);
		bitrate.setPaintLabels(true);
		bitrate.setPaintTicks(true);
		bitrate.setToolTipText(Integer.toString(bitrate.getValue()) + " Mbps");
		
		bitrate.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent ce) {
				bitrate.setToolTipText(Integer.toString(bitrate.getValue()) + " Mbps");
				bitrateLabel.setText("Maximum Bitrate = " + bitrate.getValue() + " Mbps");
			}
		});

		resolution.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Resolution newRes = (Resolution) resolution.getSelectedItem();
				bitrate.setValue(newRes.defaultBitrate);
			}
		});

		
		fullscreen = new JCheckBox("Fullscreen");
		fullscreen.setSelected(prefs.getFullscreen());
		
		allowResolutionChange = new JCheckBox("Change display resolution in fullscreen mode");
		allowResolutionChange.setSelected(prefs.getAllowResolutionChange());
		
		keepAspectRatio = new JCheckBox("Keep stream aspect ratio");
		keepAspectRatio.setSelected(prefs.isKeepAspectRatio());
		
		localAudio = new JCheckBox("Play audio on host PC");
		localAudio.setSelected(prefs.getLocalAudio());
	
		Box resolutionBox = Box.createHorizontalBox();
		resolutionBox.add(Box.createHorizontalGlue());
		resolutionBox.add(resolution);
		resolutionBox.add(Box.createHorizontalGlue());
		
        Box bitrateLabelBox = Box.createHorizontalBox();
        bitrateLabelBox.add(Box.createHorizontalGlue());
        bitrateLabelBox.add(bitrateLabel);
        bitrateLabelBox.add(Box.createHorizontalGlue());

		Box bitrateBox = Box.createHorizontalBox();
        bitrateBox.add(Box.createHorizontalGlue());
		bitrateBox.add(bitrate);
		bitrateBox.add(Box.createHorizontalGlue());
		
		Box fullscreenBox = Box.createHorizontalBox();
		fullscreenBox.add(Box.createHorizontalGlue());
		fullscreenBox.add(fullscreen);
		fullscreenBox.add(Box.createHorizontalGlue());
		
		Box allowResolutionChangeBox = Box.createHorizontalBox();
		allowResolutionChangeBox.add(Box.createHorizontalGlue());
		allowResolutionChangeBox.add(allowResolutionChange);
		allowResolutionChangeBox.add(Box.createHorizontalGlue());
		
		Box keepAspectRatioBox = Box.createHorizontalBox();
		keepAspectRatioBox.add(Box.createHorizontalGlue());
		keepAspectRatioBox.add(keepAspectRatio);
		keepAspectRatioBox.add(Box.createHorizontalGlue());
		
		Box localAudioBox = Box.createHorizontalBox();
		localAudioBox.add(Box.createHorizontalGlue());
		localAudioBox.add(localAudio);
		localAudioBox.add(Box.createHorizontalGlue());
		
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(resolutionBox);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(bitrateLabelBox);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(bitrateBox);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(fullscreenBox);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(allowResolutionChangeBox);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(keepAspectRatioBox);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(localAudioBox);
		mainPanel.add(Box.createVerticalGlue());
		
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				writePreferences(prefs);
			}
		});
		
		this.getContentPane().add(mainPanel);

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		//center on screen
		this.setLocation((int)dim.getWidth()/2-this.getWidth()/2, (int)dim.getHeight()/2-this.getHeight()/2);
		
		this.setVisible(true);
	}
	
	/*
	 * Writes the preferences to the disk.
	 */
	private void writePreferences(Preferences prefs) {
		prefs.setFullscreen(fullscreen.isSelected());
		prefs.setAllowResolutionChange(allowResolutionChange.isSelected());
		prefs.setKeepAspectRatio(keepAspectRatio.isSelected());
		prefs.setBitrate(bitrate.getValue());
		prefs.setResolution((Resolution)resolution.getSelectedItem());
		prefs.setLocalAudio(localAudio.isSelected());
		PreferencesManager.writePreferences(prefs);
	}
	
}
