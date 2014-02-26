package com.limelight.input.gamepad;

import com.limelight.LimeLog;
import com.limelight.input.Device;
import com.limelight.input.DeviceListener;
import com.limelight.input.gamepad.GamepadMapping.Mapping;
import com.limelight.input.gamepad.SourceComponent.Direction;
import com.limelight.input.gamepad.SourceComponent.Type;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.settings.GamepadSettingsManager;

/**
 * Represents a gamepad connected to the system
 * @author Diego Waxemberg
 */
public class Gamepad implements DeviceListener {

	private short inputMap = 0x0000;
	private byte leftTrigger = 0x00;
	private byte rightTrigger = 0x00;
	private short rightStickX = 0x0000;
	private short rightStickY = 0x0000;
	private short leftStickX = 0x0000;
	private short leftStickY = 0x0000;

	private NvConnection conn;

	public Gamepad(NvConnection conn) {
		this.conn = conn;
	}

	public Gamepad() {
		this(null);
	}

	public void setConnection(NvConnection conn) {
		this.conn = conn;
	}

	public void handleButton(Device device, int buttonId, boolean pressed) {
		GamepadMapping mapping = GamepadSettingsManager.getSettings();

		Mapping mapped = mapping.get(new SourceComponent(Type.BUTTON, buttonId, null));
		if (mapped == null) {
			//LimeLog.info("Unmapped button pressed: " + buttonId);
			return;
		}

		if (!mapped.padComp.isAnalog()) {
			handleDigitalComponent(mapped, pressed);
		} else {
			handleAnalogComponent(mapped.padComp, sanitizeValue(mapped, pressed));
		}

		//used for debugging
		//printInfo(device, new SourceComponent(Type.BUTTON, buttonId), mapped.padComp, pressed ? 1F : 0F);
	}

	public void handleAxis(Device device, int axisId, float newValue, float lastValue) {
		GamepadMapping mapping = GamepadSettingsManager.getSettings();
		Direction mappedDir = null;
		if (newValue == 0) {
			if (lastValue > 0) {
				mappedDir = Direction.POSITIVE;
			} else {
				mappedDir = Direction.NEGATIVE;
			}
		} else {
			mappedDir = newValue > 0 ? Direction.POSITIVE : Direction.NEGATIVE;
		}
		
		Mapping mapped = mapping.get(new SourceComponent(Type.AXIS, axisId, mappedDir));
		if (mapped == null) {
			//LimeLog.info("Unmapped axis moved: " + axisId);
			return;
		}
		float value =  sanitizeValue(mapped, newValue);

		if (mapped.padComp.isAnalog()) {
			handleAnalogComponent(mapped.padComp, value);
		} else {
			handleDigitalComponent(mapped, (Math.abs(value) > 0.5));
		}

		//used for debugging
		//printInfo(device, new SourceComponent(Type.AXIS, axisId, mappedDir), mapped.padComp, newValue);
	}


	private float sanitizeValue(Mapping mapped, boolean value) {
		if (mapped.invert) {
			return value ? 0F : 1F;
		} else {
			return value ? 1F : 0F;
		}
	}

	private float sanitizeValue(Mapping mapped, float value) {
		float retVal = value;
		if (mapped.invert) {
			retVal = -retVal;
		}
		if (mapped.trigger) {
			retVal = (retVal + 1) / 2;
		}
		return retVal;
	}

	private void handleAnalogComponent(GamepadComponent padComp, float value) {
		switch (padComp) {
		case LS_RIGHT:
		case LS_LEFT:
			leftStickX = (short)Math.round(value * 0x7FFF);
			break;
		case LS_UP:
		case LS_DOWN:
			leftStickY = (short)Math.round(value * 0x7FFF);
			break;
		case RS_UP:
		case RS_DOWN:
			rightStickX = (short)Math.round(value * 0x7FFF);
			break;
		case RS_RIGHT:
		case RS_LEFT:
			rightStickY = (short)Math.round(value * 0x7FFF);
			break;
		case LT:
			leftTrigger = (byte)Math.round(value * 0xFF);
			break;
		case RT:
			rightTrigger = (byte)Math.round(value * 0xFF);
			break;
		default:
			LimeLog.warning("A mapping error has occured. Ignoring: " + padComp.name());
			break;
		}
		if (conn != null) {
			sendControllerPacket();
		}
	}

	private void handleDigitalComponent(Mapping mapped, boolean pressed) {
		switch (mapped.padComp) {
		case BTN_A:
			toggle(ControllerPacket.A_FLAG, pressed);
			break;
		case BTN_X:
			toggle(ControllerPacket.X_FLAG, pressed);
			break;
		case BTN_Y:
			toggle(ControllerPacket.Y_FLAG, pressed);
			break;
		case BTN_B:
			toggle(ControllerPacket.B_FLAG, pressed);
			break;
		case DPAD_UP:
			toggle(ControllerPacket.UP_FLAG, pressed);
			break;
		case DPAD_DOWN:
			toggle(ControllerPacket.DOWN_FLAG, pressed);
			break;
		case DPAD_LEFT:
			toggle(ControllerPacket.LEFT_FLAG, pressed);
			break;
		case DPAD_RIGHT:
			toggle(ControllerPacket.RIGHT_FLAG, pressed);
			break;
		case LS_THUMB:
			toggle(ControllerPacket.LS_CLK_FLAG, pressed);
			break;
		case RS_THUMB:
			toggle(ControllerPacket.RS_CLK_FLAG, pressed);
			break;
		case LB:
			toggle(ControllerPacket.LB_FLAG, pressed);
			break;
		case RB:
			toggle(ControllerPacket.RB_FLAG, pressed);
			break;
		case BTN_START:
			toggle(ControllerPacket.PLAY_FLAG, pressed);
			break;
		case BTN_BACK:
			toggle(ControllerPacket.BACK_FLAG, pressed);
			break;
		case BTN_SPECIAL:
			toggle(ControllerPacket.SPECIAL_BUTTON_FLAG, pressed);
			break;
		default:
			LimeLog.warning("A mapping error has occured. Ignoring: " + mapped.padComp.name());
			return;
		}
		if (conn != null) {
			sendControllerPacket();
		}
	}

	/*
	 * Sends a controller packet to the specified connection containing the current gamepad values
	 */
	private void sendControllerPacket() {
		if (conn != null) {
			conn.sendControllerInput(inputMap, leftTrigger, rightTrigger, 
					leftStickX, leftStickY, rightStickX, rightStickY);
		}
	}

	/*
	 * Prints out the specified event information for the given gamepad
	 * used for debugging, normally unused.
	 */
	@SuppressWarnings("unused")
	private void printInfo(Device device, SourceComponent sourceComp, GamepadComponent padComp, float value) {

		StringBuilder builder = new StringBuilder();

		builder.append(sourceComp.getType().name() + ": ");
		builder.append(sourceComp.getId() + " ");
		builder.append("mapped to: " + padComp + " ");
		builder.append("changed to " + value);

		LimeLog.info(builder.toString());
	}

	/*
	 * Toggles a flag that indicates the specified button was pressed or released
	 */
	private void toggle(short button, boolean pressed) {
		if (pressed) {
			inputMap |= button;
		} else {
			inputMap &= ~button;
		}
	}

}
