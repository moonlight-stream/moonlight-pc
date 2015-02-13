package com.limelight.input.gamepad;

import java.util.HashMap;

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
public class GamepadHandler implements DeviceListener {

	private class Gamepad {
		public short controllerNumber;
		
		public GamepadMapping mapping; 
		
		public short buttonFlags;
		public byte leftTrigger;
		public byte rightTrigger;
		public short rightStickX;
		public short rightStickY;
		public short leftStickX;
		public short leftStickY;
		
		public void assignControllerNumber() {
			for (short i = 0; i < 4; i++) {
				if ((currentControllers & (1 << i)) == 0) {
					currentControllers |= (1 << i);
					LimeLog.info("Assigned controller "+i);
					controllerNumber = i;
					return;
				}
			}
			
			controllerNumber = 0;
			LimeLog.info("No controller numbers left; using 0");
		}
		
		public void releaseControllerNumber() {
			LimeLog.info("Controller "+controllerNumber+" is now available");
			currentControllers &= ~(1 << controllerNumber);
		}
	}

	private NvConnection conn;
	private HashMap<Device, Gamepad> gamepads = new HashMap<Device, Gamepad>();
	private int currentControllers;

	public GamepadHandler(NvConnection conn) {
		this.conn = conn;
	}

	public GamepadHandler() {
		this(null);
	}
	
	private Gamepad getGamepad(Device dev, boolean create) {
		Gamepad gamepad = gamepads.get(dev);
		if (gamepad != null) {
			return gamepad;
		}
		else if (create) {
			gamepad = new Gamepad();
			gamepad.mapping = GamepadSettingsManager.getSettings();
			gamepad.assignControllerNumber();
			gamepads.put(dev, gamepad);
			return gamepad;
		}
		else {
			return null;
		}
	}

	public void setConnection(NvConnection conn) {
		this.conn = conn;
	}

	public void handleButton(Device device, int buttonId, boolean pressed) {
		Gamepad gamepad = getGamepad(device, true);
		Mapping mapped = gamepad.mapping.get(new SourceComponent(Type.BUTTON, buttonId, null));
		if (mapped == null) {
			//LimeLog.info("Unmapped button pressed: " + buttonId);
			return;
		}

		if (!mapped.padComp.isAnalog()) {
			handleDigitalComponent(gamepad, mapped, pressed);
		} else {
			handleAnalogComponent(gamepad, mapped.padComp, sanitizeValue(mapped, pressed));
		}

		//used for debugging
		//printInfo(device, new SourceComponent(Type.BUTTON, buttonId), mapped.padComp, pressed ? 1F : 0F);
	}

	public void handleAxis(Device device, int axisId, float newValue, float lastValue) {
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
		
		Gamepad gamepad = getGamepad(device, true);
		Mapping mapped = gamepad.mapping.get(new SourceComponent(Type.AXIS, axisId, mappedDir));
		if (mapped == null) {
			//LimeLog.info("Unmapped axis moved: " + axisId);
			return;
		}
		float value = sanitizeValue(mapped, newValue);

		if (mapped.padComp.isAnalog()) {
			handleAnalogComponent(gamepad, mapped.padComp, value);
		} else {
			handleDigitalComponent(gamepad, mapped, (Math.abs(value) > 0.5));
		}

		//used for debugging
		//printInfo(device, new SourceComponent(Type.AXIS, axisId, mappedDir), mapped.padComp, newValue);
	}


	private static float sanitizeValue(Mapping mapped, boolean value) {
		if (mapped.invert) {
			return value ? 0F : 1F;
		} else {
			return value ? 1F : 0F;
		}
	}

	private static float sanitizeValue(Mapping mapped, float value) {
		float retVal = value;
		if (mapped.invert) {
			retVal = -retVal;
		}
		if (mapped.trigger) {
			retVal = (retVal + 1) / 2;
		}
		return retVal;
	}

	private void handleAnalogComponent(Gamepad gamepad, GamepadComponent padComp, float value) {
		switch (padComp) {
		case LS_RIGHT:
			gamepad.leftStickX = (short)(Math.abs(value) * 0x7FFE);
			break;
		case LS_LEFT:
			gamepad.leftStickX = (short)(-Math.abs(value) * 0x7FFE);
			break;
		case LS_UP:
			gamepad.leftStickY = (short)(Math.abs(value) * 0x7FFE);
			break;
		case LS_DOWN:
			gamepad.leftStickY = (short)(-Math.abs(value) * 0x7FFE);
			break;
		case RS_UP:
			gamepad.rightStickY = (short)(Math.abs(value) * 0x7FFE);
			break;
		case RS_DOWN:
			gamepad.rightStickY = (short)(-Math.abs(value) * 0x7FFE);
			break;
		case RS_RIGHT:
			gamepad.rightStickX = (short)(Math.abs(value) * 0x7FFE);
			break;
		case RS_LEFT:
			gamepad.rightStickX = (short)(-Math.abs(value) * 0x7FFE);
			break;
		case LT:
			// HACK: Fix polling so we don't have to do this
			if (Math.abs(value) < 0.9) {
				value = 0;
			}
			gamepad.leftTrigger = (byte)(Math.abs(value) * 0xFF);
			break;
		case RT:
			// HACK: Fix polling so we don't have to do this
			if (Math.abs(value) < 0.9) {
				value = 0;
			}
			gamepad.rightTrigger = (byte)(Math.abs(value) * 0xFF);
			break;
		default:
			LimeLog.warning("A mapping error has occured. Ignoring: " + padComp.name());
			break;
		}
		
		sendControllerPacket(gamepad);
	}

	private void handleDigitalComponent(Gamepad gamepad, Mapping mapped, boolean pressed) {
		switch (mapped.padComp) {
		case BTN_A:
			toggleButton(gamepad, ControllerPacket.A_FLAG, pressed);
			break;
		case BTN_X:
			toggleButton(gamepad, ControllerPacket.X_FLAG, pressed);
			break;
		case BTN_Y:
			toggleButton(gamepad, ControllerPacket.Y_FLAG, pressed);
			break;
		case BTN_B:
			toggleButton(gamepad, ControllerPacket.B_FLAG, pressed);
			break;
		case DPAD_UP:
			toggleButton(gamepad, ControllerPacket.UP_FLAG, pressed);
			break;
		case DPAD_DOWN:
			toggleButton(gamepad, ControllerPacket.DOWN_FLAG, pressed);
			break;
		case DPAD_LEFT:
			toggleButton(gamepad, ControllerPacket.LEFT_FLAG, pressed);
			break;
		case DPAD_RIGHT:
			toggleButton(gamepad, ControllerPacket.RIGHT_FLAG, pressed);
			break;
		case LS_THUMB:
			toggleButton(gamepad, ControllerPacket.LS_CLK_FLAG, pressed);
			break;
		case RS_THUMB:
			toggleButton(gamepad, ControllerPacket.RS_CLK_FLAG, pressed);
			break;
		case LB:
			toggleButton(gamepad, ControllerPacket.LB_FLAG, pressed);
			break;
		case RB:
			toggleButton(gamepad, ControllerPacket.RB_FLAG, pressed);
			break;
		case BTN_START:
			toggleButton(gamepad, ControllerPacket.PLAY_FLAG, pressed);
			break;
		case BTN_BACK:
			toggleButton(gamepad, ControllerPacket.BACK_FLAG, pressed);
			break;
		case BTN_SPECIAL:
			toggleButton(gamepad, ControllerPacket.SPECIAL_BUTTON_FLAG, pressed);
			break;
		default:
			LimeLog.warning("A mapping error has occured. Ignoring: " + mapped.padComp.name());
			return;
		}
		
		sendControllerPacket(gamepad);
	}

	/*
	 * Sends a controller packet to the specified connection containing the current gamepad values
	 */
	private void sendControllerPacket(Gamepad gamepad) {
		if (conn != null) {
			conn.sendControllerInput(gamepad.controllerNumber, gamepad.buttonFlags, gamepad.leftTrigger, gamepad.rightTrigger, 
					gamepad.leftStickX, gamepad.leftStickY, gamepad.rightStickX, gamepad.rightStickY);
		}
	}

	/*
	 * Prints out the specified event information for the given gamepad
	 * used for debugging, normally unused.
	 */
	@SuppressWarnings("unused")
	private static void printInfo(Device device, SourceComponent sourceComp, GamepadComponent padComp, float value) {

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
	private static void toggleButton(Gamepad gamepad, short button, boolean pressed) {
		if (pressed) {
			gamepad.buttonFlags |= button;
		} else {
			gamepad.buttonFlags &= ~button;
		}
	}

	@Override
	public void handleDeviceAdded(Device device) {
		// Causes the creation of the gamepad object
		getGamepad(device, true);
	}

	@Override
	public void handleDeviceRemoved(Device device) {
		Gamepad gamepad = getGamepad(device, false);
		if (gamepad != null) {
			gamepad.releaseControllerNumber();
		}
	}

}
