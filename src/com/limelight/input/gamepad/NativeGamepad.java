package com.limelight.input.gamepad;

import java.util.ArrayList;

public class NativeGamepad {
	public static final int DEFAULT_DEVICE_POLLING_ITERATIONS = 100;
	public static final int DEFAULT_EVENT_POLLING_INTERVAL = 20;
	
	private static ArrayList<NativeGamepadListener> listenerList =
			new ArrayList<NativeGamepadListener>();
	private static boolean running = false;
	private static boolean initialized = false;
	private static Thread pollingThread = null;
	private static int devicePollingIterations = DEFAULT_DEVICE_POLLING_ITERATIONS;
	private static int pollingIntervalMs = DEFAULT_EVENT_POLLING_INTERVAL;

	static {
		System.loadLibrary("gamepad_jni");
	}
	
	private static native void init();
	private static native void shutdown();
	private static native int numDevices();
	private static native void detectDevices();
	private static native void processEvents();
	
	public static void addListener(NativeGamepadListener listener) {
		listenerList.add(listener);
	}
	
	public static void removeListener(NativeGamepadListener listener) {
		listenerList.remove(listener);
	}
	
	public static boolean isRunning() {
		return running;
	}
	
	public static void setDevicePollingIterations(int iterations) {
		devicePollingIterations = iterations;
	}
	
	public static int getDevicePollingIterations() {
		return devicePollingIterations;
	}
	
	public static void setPollingInterval(int interval) {
		pollingIntervalMs = interval;
	}
	
	public static int getPollingInterval() {
		return pollingIntervalMs;
	}
	
	public static void start() {
		if (!running) {
			startPolling();
			running = true;
		}
	}
	
	public static void stop() {
		if (running) {
			stopPolling();
			running = false;
		}
	}
	
	public static void release() {
		if (running) {
			throw new IllegalStateException("Cannot release running NativeGamepad");
		}
		
		if (initialized) {
			NativeGamepad.shutdown();
			initialized = false;
		}
	}
	
	public static int getDeviceCount() {
		if (!running) {
			throw new IllegalStateException("NativeGamepad not running");
		}
		
		return NativeGamepad.numDevices();
	}
	
	private static void startPolling() {
		pollingThread = new Thread() {
			@Override
			public void run() {
				int iterations = 0;
				
				if (!initialized) {
					NativeGamepad.init();
					initialized = true;
				}
				
				while (!isInterrupted()) {
					if ((iterations++ % devicePollingIterations) == 0) {
						NativeGamepad.detectDevices();
					}
					
					NativeGamepad.processEvents();
					
					try {
						Thread.sleep(pollingIntervalMs);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		};
		pollingThread.setName("Native Gamepad - Polling Thread");
		pollingThread.start();
	}
	
	private static void stopPolling() {
		if (pollingThread != null) {
			pollingThread.interrupt();
			
			try {
				pollingThread.join();
			} catch (InterruptedException e) {}
		}
	}
	
	public static void deviceAttachCallback(int deviceId, int numButtons, int numAxes) {
		System.out.println(deviceId + " has attached.");
		for (NativeGamepadListener listener : listenerList) {
			listener.deviceAttached(deviceId, numButtons, numAxes);
		}
	}
	
	public static void deviceRemoveCallback(int deviceId) {
		System.out.println(deviceId + " has detached.");
		for (NativeGamepadListener listener : listenerList) {
			listener.deviceRemoved(deviceId);
		}
	}
	
	public static void buttonUpCallback(int deviceId, int buttonId) {
		for (NativeGamepadListener listener : listenerList) {
			listener.buttonUp(deviceId, buttonId);
		}
	}
	
	public static void buttonDownCallback(int deviceId, int buttonId) {
		for (NativeGamepadListener listener : listenerList) {
			listener.buttonDown(deviceId, buttonId);
		}
	}
	
	public static void axisMovedCallback(int deviceId, int axisId, float value, float lastValue) {
		for (NativeGamepadListener listener : listenerList) {
			listener.axisMoved(deviceId, axisId, value, lastValue);
		}
	}
}
