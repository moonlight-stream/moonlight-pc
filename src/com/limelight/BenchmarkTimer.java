package com.limelight;

public class BenchmarkTimer {

	private int timers;
	private long total;
	private long lastTime;
	private long lastStarted;
	
	public BenchmarkTimer() {
		clear();
	}
	
	public long startTimer() {
		if (lastStarted == -1l)
			lastStarted = System.currentTimeMillis();
		return lastStarted;
	}
	
	public long stopTimer() {
		long current = System.currentTimeMillis();
		
		if (lastStarted == -1l) return -1l;
		
		lastTime = current - lastStarted;
		total += lastTime;
		lastStarted = -1l;
		timers++;
		return lastTime;
	}

	public int getTimersCount() {
		return timers;
	}

	public long getLastTime() {
		return lastTime;
	}
	
	public long getAverageTime() {
		if (timers == 0) {
			return 0l; 
		}
		return total / timers;
	}
	
	public void clear() {
		lastStarted = -1l;
		lastTime = 0l;
		total = 0l;
		timers = 0;
	}

	public long getTotalTime() {
		return total;
	}
}
