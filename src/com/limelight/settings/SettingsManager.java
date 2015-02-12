package com.limelight.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.limelight.LimeLog;

import java.io.*;

/**
 * Manages the settings files
 * @author Diego Waxemberg
 */
public class SettingsManager {
	/**
	 * Directory to which settings will be saved
	 */
	public static String SETTINGS_DIR = System.getProperty("user.home") + File.separator + "Limelight";
	
	//directory to hold limelight settings
	private File settingsDir;
	
	private File settingsFile;
	private File gamepadFile;
	
	private static SettingsManager manager;
	
	/*
	 * Constructs a manager that initializes the settings files
	 */
	private SettingsManager() {
		settingsFile = new File(SETTINGS_DIR + File.separator + "settings.json");
		gamepadFile = new File(SETTINGS_DIR + File.separator + "gamepad.json");
		settingsDir = new File(SETTINGS_DIR);
	}
	
	/**
	 * Gets an instance of the manager, this is a singleton
	 * @return the instance of the manager
	 */
	public static SettingsManager getInstance() {
		if (manager == null) {
			manager = new SettingsManager();
		}
		return manager;
	}
	
	/**
	 * Gets the gamepad preference file, if the file does not exist, it is created first
	 * @return the gamepad preference file
	 */
	public File getGamepadFile() {
		if (!settingsDir.exists()) {
			settingsDir.mkdirs();
		}
		
		if (!gamepadFile.exists()) {
			try {
				gamepadFile.createNewFile();
			} catch (IOException e) {
				LimeLog.warning("Unable to create gamepad file");
				return null;
			}
		}
		
		return gamepadFile;
	}
	
	/**
	 * Gets the settings file (user preferences), if the file does not exist, it is created first
	 * @return the settings file
	 */
	public File getSettingsFile() {
		if (!settingsDir.exists()) {
			settingsDir.mkdirs();
		}
		
		if (!settingsFile.exists()) {
			try {
				settingsFile.createNewFile();
			} catch (IOException e) {
				LimeLog.warning("Unable to create setting file");
				return null;
			}
		}
		
		return settingsFile;
	}
	
	/**
	 * Reads the specified file as a settings file and returns the result.
	 * <br>A settings file must be a java serialized object
	 * @param <T>
	 * @param file the file to read in
	 * @return the settings represented in this file
	 */
	public static <T> Object readSettings(File file, Class<T> klass) {
		Gson gson = new GsonBuilder()
          .setPrettyPrinting().enableComplexMapKeySerialization().create();
		T settings = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			settings = gson.fromJson(br, klass);
		} catch (FileNotFoundException e) {
			LimeLog.warning("Could not find " + file.getName() + " settings file");
			e.printStackTrace();
		} catch (JsonSyntaxException e) {
			LimeLog.warning("JSON settings are corrupt; returning null");
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LimeLog.warning("Could not close gamepad settings file");
					e.printStackTrace();
				}
			}
		}
		return settings;
	}
	
	/**
	 * Writes the specified settings to the desired file
	 * @param file the file to write the settings to 
	 * @param settings the settings to be written out
	 */
	public static <T extends Serializable> void writeSettings(File file, T settings) {
		Gson gson = new GsonBuilder()
			.setPrettyPrinting().enableComplexMapKeySerialization().create();
		FileWriter writer = null;

		try {
			String json = gson.toJson(settings);
			writer = new FileWriter(file);
			writer.write(json);
		} catch (FileNotFoundException e) {
			LimeLog.warning("Could not find " + file.getName() + " settings file");
			e.printStackTrace();

		} catch (IOException e) {
			LimeLog.warning("Could not write to " + file.getName() + " settings file");
			e.printStackTrace();

		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					LimeLog.warning("Unable to close " + file.getName() + " settings file");
					e.printStackTrace();
				}
			}
		}
	}
}
