package com.limelight.input.gamepad;

import java.io.Serializable;

import javax.swing.JLabel;

/**
 * Enumerator for every gamepad component GFE recognizes
 * @author Diego Waxemberg
 */
public enum GamepadComponent implements Serializable {
	BTN_A("Button 1 (A)", false), BTN_X("Button 2 (X)", false), BTN_Y("Button 3 (Y)", false), BTN_B("Button 4 (B)", false), 
	DPAD_UP("D-pad Up", false), DPAD_DOWN("D-pad Down", false), DPAD_LEFT("D-pad Left", false), DPAD_RIGHT("D-pad Right", false),
	LS_RIGHT("Left Stick Right", true), LS_LEFT("Left Stick Left", true), LS_UP("Left Stick Up", true), LS_DOWN("Left Stick Down", true),
	RS_RIGHT("Right Stick Right", true), RS_LEFT("Right Stick Left", true), RS_UP("Right Stick Up", true), RS_DOWN("Right Stick Down", true),
	LS_THUMB("Left Stick Button", false), RS_THUMB("Right Stick Button", false),
	LT("Left Trigger", true), RT("Right Trigger", true), LB("Left Bumper", false), RB("Right Bumper", false), 
	BTN_START("Start Button", false), BTN_BACK("Back Button", false), BTN_SPECIAL("Special Button", false);
	
	private JLabel label;
	private boolean analog;
	
	/*
	 * Constructs the enumerator with the given name for a label and whether it is analog or not
	 */
	private GamepadComponent(String name, boolean analog) {
		this.label = new JLabel(name);
		this.analog = analog;
	}
	
	/**
	 * Gets the label for this gamepad component
	 * @return a label with the name of this component as the text
	 */
	public JLabel getLabel() {
		return label;
	}
	
	/**
	 * Checks if this component is analog or digital
	 * @return whether this component is analog
	 */
	public boolean isAnalog() {
		return analog;
	}
}
