package com.limelight.input.gamepad;

import java.io.Serializable;

public class SourceComponent implements Serializable {
	private static final long serialVersionUID = 2366409458045238273L;

	public enum Type { AXIS, BUTTON }
	public enum Direction { POSITIVE, NEGATIVE }
	private Type type;
	private Direction dir;
	private int id;
	
	public SourceComponent(Type type, int id, Direction dir) {
		this.type = type;
		this.id = id;
		this.dir = dir;
	}
	
	public Type getType() {
		return type;
	}
	
	public int getId() {
		return id;
	}
	
	public Direction getDirection() {
		return dir;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((dir == null) ? 0 : dir.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || !(obj instanceof SourceComponent)) {
			return false;
		}
		SourceComponent other = (SourceComponent) obj;
	
		return id == other.id && type == other.type && dir == other.dir;
	}
}
