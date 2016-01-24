package com.limelight.input.gamepad;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map.Entry;

import com.limelight.input.gamepad.SourceComponent.Direction;
import com.limelight.input.gamepad.SourceComponent.Type;

/**
 * Mappings for gamepad components
 * @author Diego Waxemberg
 */
public class GamepadMapping implements Serializable {
	private static final long serialVersionUID = -185035113915743149L;
	
	private HashMap<SourceComponent, Mapping> mapping;

	/**
	 * Constructs a new mapping that has nothing mapped.
	 */
	public GamepadMapping() {
		mapping = new HashMap<SourceComponent, Mapping>();
	}
	
	/**
	 * Inserts the specified mapping into this map
	 * @param toMap a <code>Mapping</code> that will be mapped to the specified gamepad component
	 * @param comp the gamepad component to map to.
	 */
	public void insertMapping(Mapping toMap, SourceComponent comp) {
		mapping.put(comp, toMap);
	}
	
	/**
	 * Gets the mapping for the specified gamepad component
	 * @param comp the gamepad component to get a mapping for
	 * @return a mapping for the requested component
	 */
	public Mapping get(SourceComponent comp) {
		return mapping.get(comp);
	}
	
	/**
	 * Removes the mapping to the specified component
	 * @param comp the component to no longer be mapped.
	 */
	public void remove(SourceComponent comp) {
		mapping.remove(comp);
	}
	
	/**
	 * Gets the mapped ControllerComponent for the specified ControllerComponent.</br>
	 * <b>NOTE: Iterates a hashmap, use sparingly</b>
	 * @param padComp the component to get a mapping for
	 * @return a mapping or an null if there is none
	 */
	public Mapping get(GamepadComponent padComp) {
		//#allTheJank
		for (Entry<SourceComponent, Mapping> entry : mapping.entrySet()) {
			if (entry.getValue().padComp == padComp) {
				return entry.getValue();
			}
		}
		return null;
	}
	
	/**
	 * Gets the mapping for the specified component.</br>
	 * <b>NOTE: Iterates a hashmap, use sparingly</b>
	 * @param padComp the component to get a mapping for
	 * @return a mapping or an empty string if there is none
	 */
	public SourceComponent getMapping(GamepadComponent padComp) {
		for (Entry<SourceComponent, Mapping> entry : mapping.entrySet()) {
			if (entry.getValue().padComp == padComp) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	/**
	 * Represents a mapping, that is which gamepad component, whether it is inverted, a trigger, etc.
	 * @author Diego Waxemberg
	 */
	public class Mapping implements Serializable {
		private static final long serialVersionUID = -8407172977953214242L;
		
		/**
		 * The component this mapping belongs to
		 */
		public GamepadComponent padComp;
		
		/**
		 * Whether the value of this component should be inverted
		 */
		public boolean invert;
		
		/**
		 * Whether this component should be treated as a trigger
		 */
		public boolean trigger;
		
		/**
		 * Constructs a new mapping with the specified configuration
		 * @param padComp the component this mapping belongs to
		 * @param invert whether the value should be inverted
		 * @param trigger whether this component should be treated as a trigger
		 */
		public Mapping(GamepadComponent padComp, boolean invert, boolean trigger) {
			this.padComp = padComp;
			this.invert = invert;
			this.trigger = trigger;
		}
	}
	
	/**
	 * Generates a default mapping using the Windows XInput bindings
	 * @return a default mapping
	 */
	public static GamepadMapping getWindowsDefaultMapping() {
		GamepadMapping defaultMap = new GamepadMapping();
		
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.BTN_A, false, false), new SourceComponent(Type.BUTTON, 10, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.BTN_B, false, false), new SourceComponent(Type.BUTTON, 11, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.BTN_X, false, false), new SourceComponent(Type.BUTTON, 12, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.BTN_Y, false, false), new SourceComponent(Type.BUTTON, 13, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.BTN_BACK, false, false), new SourceComponent(Type.BUTTON, 5, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.BTN_START, false, false), new SourceComponent(Type.BUTTON, 4, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.DPAD_DOWN, false, false), new SourceComponent(Type.BUTTON, 1, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.DPAD_LEFT, false, false), new SourceComponent(Type.BUTTON, 2, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.DPAD_RIGHT, false, false), new SourceComponent(Type.BUTTON, 3, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.DPAD_UP, false, false), new SourceComponent(Type.BUTTON, 0, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.LB, false, false), new SourceComponent(Type.BUTTON, 8, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.LT, false, false), new SourceComponent(Type.AXIS, 4, Direction.POSITIVE));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.LS_THUMB, false, false), new SourceComponent(Type.BUTTON, 6, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.LS_RIGHT, false, false), new SourceComponent(Type.AXIS, 0, Direction.POSITIVE));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.LS_LEFT, false, false), new SourceComponent(Type.AXIS, 0, Direction.NEGATIVE));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.LS_UP, false, false), new SourceComponent(Type.AXIS, 1, Direction.POSITIVE));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.LS_DOWN, false, false), new SourceComponent(Type.AXIS, 1, Direction.NEGATIVE));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.RB, false, false), new SourceComponent(Type.BUTTON, 9, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.RT, false, false), new SourceComponent(Type.AXIS, 5, Direction.POSITIVE));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.RS_THUMB, false, false), new SourceComponent(Type.BUTTON, 7, null));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.RS_RIGHT, false, false), new SourceComponent(Type.AXIS, 2, Direction.POSITIVE));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.RS_LEFT, false, false), new SourceComponent(Type.AXIS, 2, Direction.NEGATIVE));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.RS_UP, false, false), new SourceComponent(Type.AXIS, 3, Direction.POSITIVE));
		defaultMap.insertMapping(defaultMap.new Mapping(GamepadComponent.RS_DOWN, false, false), new SourceComponent(Type.AXIS, 3, Direction.NEGATIVE));

		return defaultMap;
	}
}
