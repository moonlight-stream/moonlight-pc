package com.limelight.settings;

import com.limelight.LimeLog;

import java.io.File;
import java.io.Serializable;
import java.util.Random;

/**
 * Manages user preferences
 * @author Diego Waxemberg
 */
public abstract class PreferencesManager {
	private static Preferences cachedPreferences = null;

	/**
	 * Writes the specified preferences to the preferences file and updates the cached preferences.
	 * @param prefs the preferences to be written out
	 */
	public static void writePreferences(Preferences prefs) {
		LimeLog.info("Writing Preferences");
		File prefFile = SettingsManager.getInstance().getSettingsFile();

		SettingsManager.writeSettings(prefFile, prefs);
		cachedPreferences = prefs;
	}

	/**
	 * Checks if the preferences file exists
	 * @return true if preferences exist
	 */
	public static boolean hasExistingPreferences() {
		File prefFile = SettingsManager.getInstance().getSettingsFile();
		return SettingsManager.readSettings(prefFile, Preferences.class) != null;
	}

	/**
	 * Reads the user preferences from the preferences file and caches them
	 * @return the user preferences
	 */
	public static Preferences getPreferences() {
		if (cachedPreferences == null) {
			LimeLog.info("Reading Preferences");
			File prefFile = SettingsManager.getInstance().getSettingsFile();
			Preferences savedPref = (Preferences)SettingsManager.readSettings(prefFile, Preferences.class);
			cachedPreferences = savedPref;
		}
		if (cachedPreferences == null) {
			LimeLog.warning("Unable to get preferences, using default");
			cachedPreferences = new Preferences();
			writePreferences(cachedPreferences);
		}
		return cachedPreferences;
	}

	/**
	 * Represents a user's preferences
	 * @author Diego Waxemberg
	 */
	public static class Preferences implements Serializable {
		private static final long serialVersionUID = -5575445156207845705L;

		/**
		 * The possible resolutions for the stream
		 */
		public enum Resolution { RES_720_30(1280, 720, 30, 5), RES_720_60(1280, 720, 60, 10),
			RES_1080_30(1920, 1080, 30, 10), RES_1080_60(1920, 1080, 60, 20);
			public int width;
			public int height;
			public int frameRate;
			public int defaultBitrate;
			
			/**
			 * Creates a new resolution with the specified name
			 */
			private Resolution(int width, int height, int frameRate, int defaultBitrate) {
				this.width = width;
				this.height = height;
				this.frameRate = frameRate;
				this.defaultBitrate = defaultBitrate;
			}

			/**
			 * Gets the specified name for this resolution
			 * @return the specified name of this resolution
			 */
			@Override
			public String toString() {
				return String.format("%dx%d (%dHz)", width, height, frameRate);
			}
			
			public static Resolution findRes(int height, int refresh) {
			    for (Resolution res : Resolution.values()) {
			        if (res.height == height && res.frameRate == refresh) {
			            return res;
			        }
			    }
			    return null;
			}
		};

		private Resolution res;
		private int bitrate;
		private boolean fullscreen;
		private String host;
		private String uniqueId;
		private boolean localAudio;
		private boolean allowResolutionChange;
		private boolean keepAspectRatio;

		/**
		 * constructs default preferences: 720p 60Hz
		 * full-screen will be default for Windows (where it always runs properly)
		 * windowed will be default for other platforms
		 */
		public Preferences() {
			this(Resolution.RES_720_60,
                 System.getProperty("os.name", "").contains("Windows"));
		}

		/**
		 * Constructs a preference with the specified values
		 * @param res the <code>Resolution</code> to use
		 * @param fullscreen whether to start the stream in fullscreen
		 */
		private Preferences(Resolution res, boolean fullscreen) {
			this.res = res;
			this.bitrate = res.defaultBitrate;
			this.fullscreen = fullscreen;
			this.host = "GeForce PC host";
			this.uniqueId = String.format("%016x", new Random().nextLong());
			this.localAudio = false;
			this.allowResolutionChange = true;
			this.keepAspectRatio = true;
		}

		/**
		 * The saved host in this preference
		 * @return the last used host
		 */
		public String getHost() {
			return host;
		}

		/**
		 * Sets the host for this preference
		 * @param host the host to save
		 */
		public void setHost(String host) {
			this.host = host;
		}

		/**
		 * Gets the resolution in this preference
		 * @return the stored resolution
		 */
		public Resolution getResolution() {
			// We removed some resolution values, so fixup the resolution
			// if the enum value couldn't be found
			if (res == null) {
				res = Resolution.RES_720_60;
			}
			return res;
		}

		/**
		 * Gets the bitrate in this preference
		 * @return the stored bitrate
		 */
		public int getBitrate() {
			return bitrate;
		}
		
		/**
		 * Gets whether to use fullscreen
		 * @return the stored fullscreen mode
		 */
		public boolean getFullscreen() {
			return fullscreen;
		}
		
		/**
		 * Gets whether to use local audio
		 * @return the stored localAudio
		 */
		public boolean getLocalAudio() {
			return localAudio;
		}

		/**
		 * Sets the resolution in this preference
		 * @param res the resolution to save
		 */
		public void setResolution(Resolution res) {
			this.res = res;
		}

		/**
		 * Sets the bitrate in this preference
		 * @param bitrate the bitrate to save
		 */
		public void setBitrate(int bitrate) {
			this.bitrate = bitrate;
		}
		
		/**
		 * Sets the fullscreen mode of this preference
		 * @param fullscreen whether to use fullscreen
		 */
		public void setFullscreen(boolean fullscreen) {
			this.fullscreen = fullscreen;
		}
		
		/**
		 * Gets the unique ID
		 * @return uniqueId the unique ID
		 */
		public String getUniqueId() {
			return uniqueId;
		}
		
		/**
		 * Sets the local audio use of this preference
		 * @param localAudio whether to use localAudio
		 */
		public void setLocalAudio(boolean localAudio) {
			this.localAudio = localAudio;
		}

		
		public boolean getAllowResolutionChange() {
			return allowResolutionChange;
		}
		
		public void setAllowResolutionChange(boolean allowResolutionChange) {
			this.allowResolutionChange = allowResolutionChange;
		}

		public boolean isKeepAspectRatio() {
			return keepAspectRatio;
		}

		public void setKeepAspectRatio(boolean keepAspectRatio) {
			this.keepAspectRatio = keepAspectRatio;
		}
		
		
	}
}
