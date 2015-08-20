package com.limelight;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLine;

import com.limelight.binding.LibraryHelper;
import com.limelight.binding.PlatformBinding;
import com.limelight.gui.MainFrame;
import com.limelight.gui.StreamFrame;
import com.limelight.input.gamepad.GamepadHandler;
import com.limelight.input.gamepad.GamepadListener;
import com.limelight.input.gamepad.NativeGamepad;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.NvConnectionListener;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.settings.PreferencesManager;
import com.limelight.settings.PreferencesManager.Preferences;
import com.limelight.settings.PreferencesManager.Preferences.Resolution;
import com.limelight.settings.SettingsManager;

/**
 * Main class for Limelight-pc contains methods for starting the application as well
 * as the stream to the host pc.
 * @author Diego Waxemberg<br>
 * Cameron Gutman
 */
public class Limelight implements NvConnectionListener {
	public static final double VERSION = 1.0;
	public static boolean COMMAND_LINE_LAUNCH = false;

	private String host;
	private StreamFrame streamFrame;
	private NvConnection conn;
	private boolean connectionTerminating;
	private static JFrame limeFrame;
	private GamepadHandler gamepad;
	private VideoDecoderRenderer decoderRenderer;
	
	public static void displayUiMessage(JFrame frame, String message, String title, int type) {
		if (COMMAND_LINE_LAUNCH) {
			if (type == JOptionPane.ERROR_MESSAGE) {
				System.err.println(message);
			}
			else {
				System.out.println(message);
			}
		}
		else {
			JOptionPane.showMessageDialog(frame, message, title, type);
		}
	}

	/**
	 * Constructs a new instance based on the given host
	 * @param host can be hostname or IP address.
	 */
	public Limelight(String host) {
		this.host = host;
	}

	/*
	 * Creates a connection to the host and starts up the stream.
	 */
	public void startUp(StreamConfiguration streamConfig, Preferences prefs) {
		streamFrame = new StreamFrame();

		decoderRenderer = PlatformBinding.getVideoDecoderRenderer();
		
		conn = new NvConnection(host, prefs.getUniqueId(), this, streamConfig, PlatformBinding.getCryptoProvider());
		streamFrame.build(this, conn, streamConfig, prefs.getFullscreen());
		conn.start(PlatformBinding.getDeviceName(), streamFrame,
				VideoDecoderRenderer.FLAG_PREFER_QUALITY,
				PlatformBinding.getAudioRenderer(),
				decoderRenderer);
	}

	/*
	 * Creates a StreamConfiguration given a Resolution. 
	 * Used to specify what kind of stream will be used.
	 */
	public static StreamConfiguration createConfiguration(Resolution res, Integer bitRate, String appName) {
		return new StreamConfiguration.Builder()
		.setApp(new NvApp(appName))
		.setResolution(res.width, res.height)
		.setRefreshRate(res.frameRate)
		.setBitrate(bitRate*1000).build();
	}

	/*
	 * Creates the main frame for the application.
	 */
	private static void createFrame() {
		// Tell the user how to map the gamepad if it's a new install and there's no default for this platform
		if (!PreferencesManager.hasExistingPreferences() &&
				!System.getProperty("os.name").contains("Windows")) {
			displayUiMessage(null, "Gamepad mapping is not set. If you want to use a gamepad, "+
					"click the Options menu and choose Gamepad Settings. After mapping your gamepad,"+
					" it will work while streaming.", "Moonlight", JOptionPane.INFORMATION_MESSAGE);
		}

		MainFrame main = new MainFrame();
		main.build();
		limeFrame = main.getLimeFrame();
	}

	/**
	 * Load native libraries for this platform or show an error dialog
	 * @return Error message or null for success
	 */
	public static String loadNativeLibraries() {
		String errorMessage;

		try {
			String libraryPlatform = LibraryHelper.getLibraryPlatformString();
			String jrePlatform = LibraryHelper.getRunningPlatformString();

			if (libraryPlatform.equals(jrePlatform)) {
				// Success path
				LibraryHelper.prepareNativeLibraries();
				NativeGamepad.addListener(GamepadListener.getInstance());
				NativeGamepad.start();
				return null;
			}
			else {
				errorMessage = "This is not the correct JAR for your platform. Please download the \""+jrePlatform+"\" JAR.";
			}
		} catch (IOException e) {
			errorMessage = "The JAR is malformed or an invalid native library path was specified.";
		}

		return errorMessage;
	}

	/**
	 * Creates a new instance and starts the stream.
	 * @param host the host pc to connect to. Can be a hostname or IP address.
	 */
	public static void createInstance(String host, String appName) {
		Limelight limelight = new Limelight(host);

		Preferences prefs = PreferencesManager.getPreferences();
		StreamConfiguration streamConfig = createConfiguration(prefs.getResolution(), prefs.getBitrate(), appName);

		limelight.startUp(streamConfig, prefs);
	}

	/**
	 * The entry point for the application. <br>
	 * Does some initializations and then creates the main frame.
	 * @param args unused.
	 */
	public static void main(String args[]) {
		// Redirect logging to a file if we're running from a JAR
		if (LibraryHelper.isRunningFromJar() && args.length == 0) {
			try {
				LimeLog.setFileHandler(SettingsManager.SETTINGS_DIR + File.separator + "limelight.log");
			} catch (IOException e) {
				System.err.println("ERROR! Unable to set log's file handler in Limelight.java's Main function");
			}
		}

		//fix the menu bar if we are running in osx
		if (System.getProperty("os.name").contains("Mac OS X")) {
			// set the name of the application menu item
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Moonlight");
		}

		String libraryError = loadNativeLibraries();

		if (libraryError == null) {
			// launching with command line arguments
			if (args.length == 0) {
				createFrame();
			}
			else {
				parseCommandLine(args);
			}
		} else {
			displayUiMessage(null, libraryError, "Wrong JAR platform", JOptionPane.ERROR_MESSAGE);
		}
	}

	private static void parseCommandLine(String[] args) {
		String host = null;
		boolean fullscreen = false;
		int resolution = 720;
		int refresh = 60;
		Integer bitrate = null;
		String appName = "Steam";
		
		Preferences prefs = PreferencesManager.getPreferences();
		
		// Save preferences to preserve possibly new unique ID
		PreferencesManager.writePreferences(prefs);

		CommandLine commandLine;//Apache's CLI stuff
		//for future reference: Option(String switch, does the option have args, String description)
		Option optionPair = new Option("pair", true, "hostname or IP address to pair to");
		Option optionHost = new Option("host", true, "hostname or IP address to connect to (required)");
		Option optionBitrate = new Option("bitrate", true, "stream bit rate, in Megabits per second (Mbps)");
		Option optionApp = new Option("app", true, "application to run on start, defaulted to 'Steam'");
		Option optionFS = new Option("fs", false, "run steam in fullscreen");
		Option option720 = new Option("720", false, "set stream resolution to 720p");
		Option option768 = new Option("768", false, "set stream resolution to 768p");
		Option option900 = new Option("900", false, "set stream resolution to 900p");
		Option option1080 = new Option("1080", false, "set stream resolution to 1080p");
		Option option30FPS = new Option("30fps", false, "set stream refresh rate to 30 fps");
		Option option60FPS = new Option("60fps", false, "set stream resolution to 60 fps");
		
		Options options = new Options().addOption(optionPair).addOption(optionHost).addOption(optionBitrate).addOption(optionApp)
				.addOption(optionFS).addOption(option720).addOption(option768).addOption(option900)
				.addOption(option1080).addOption(option1080).addOption(option30FPS).addOption(option60FPS);
		CommandLineParser parser = new DefaultParser();
		
		try{
			commandLine = parser.parse(options, args, true); 
			//parse the args into the options, and stop parsing at unrecognized token

			if (commandLine.hasOption(optionPair.getOpt())) {//parse the 'pair' switch.  This will begin pairing, then exit once completed.
				String result = commandLine.getOptionValue(optionPair.getOpt());
				if(result == null || result.startsWith("-")){//this is a legal assumption under RFC 952 and 1123 did not change it.
					System.err.println("Syntax error: -"+optionPair.getOpt()+" requires argument "+ optionPair.getDescription());
					System.exit(4);
				}
				host = result;
				System.out.println("Trying to pair to: " + host);
				String msg = pair(prefs.getUniqueId(), host);
				System.out.println("Pairing: " + msg);
				System.exit(0);
			}
			
			if (commandLine.hasOption(optionHost.getOpt())){//parse the 'host' switch.  This is the hostname or IP to connect to
				String result = commandLine.getOptionValue(optionHost.getOpt());
				if(result == null || result.startsWith("-")){//this is a legal assumption under RFC 952 and 1123 did not change it.
					System.err.println("Syntax error: -"+optionHost.getOpt()+" requires argument "+ optionHost.getDescription());
					System.exit(5);
				}
				host = result;
			}
			
			if(commandLine.hasOption(optionBitrate.getOpt())){
				String result = commandLine.getOptionValue(optionBitrate.getOpt());
				if(result == null || result.startsWith("-")){//parse the 'bitrate' switch.
					System.err.println("Syntax error: -"+optionBitrate.getOpt()+" requires argument "+ optionBitrate.getDescription());
					System.exit(3);
				}
				bitrate = Integer.parseInt(result);
			}
			
			if(commandLine.hasOption(optionApp.getOpt())){
				String result = commandLine.getOptionValue(optionApp.getOpt());
				if(result == null || result.startsWith("-")){//parse the 'app' switch.  
					//I make the assumption that an 'app' doesn't begin with a hyphen, because that makes everything nice.
					System.err.println("Syntax error: -"+optionApp.getOpt()+" requires argument "+ optionApp.getDescription());
					System.exit(3);
				}
				appName = result;
			}
			
			fullscreen = commandLine.hasOption(optionFS.getOpt());
			if(commandLine.hasOption(option720.getOpt())) resolution = 720;
			if(commandLine.hasOption(option768.getOpt())) resolution = 768;
			if(commandLine.hasOption(option900.getOpt())) resolution = 900;
			if(commandLine.hasOption(option1080.getOpt())) resolution = 1080;
			if(commandLine.hasOption(option30FPS.getOpt())) refresh = 30;
			if(commandLine.hasOption(option60FPS.getOpt())) refresh = 60;
			
		} catch (org.apache.commons.cli.ParseException exception){
			System.err.println(exception.toString());
		} catch (NumberFormatException exception) {
			System.err.println("ERROR! Bad bitrate given!");
			bitrate = null;
		}

		Resolution streamRes = Resolution.findRes(resolution, refresh);
		
		if (bitrate == null) {
			bitrate = streamRes.defaultBitrate;
		}

		StreamConfiguration streamConfig = createConfiguration(streamRes, bitrate, appName);
		
		prefs.setResolution(streamRes);
		prefs.setBitrate(bitrate);
		prefs.setFullscreen(fullscreen);
		
		Limelight limelight = new Limelight(host);
		limelight.startUp(streamConfig, prefs);
		COMMAND_LINE_LAUNCH = true;
	}


	public void stop() {
		connectionTerminating = true;

		// Kill the connection to the target
		conn.stop();

		// Remove the gamepad listener
		if (gamepad != null) {
			GamepadListener.getInstance().removeListener(gamepad);
		}
		
		int endToEndLatency = decoderRenderer.getAverageEndToEndLatency();
		if (endToEndLatency != 0) {
			displayMessage(String.format("Average client latency: %d ms",
					endToEndLatency));
		}

		// Close the stream frame
		streamFrame.dispose();
	}

	/**
	 * Callback to specify which stage is starting. Used to update UI.
	 * @param stage the Stage that is starting
	 */
	public void stageStarting(Stage stage) {
		LimeLog.info("Starting "+stage.getName());
		streamFrame.showSpinner(stage);
	}

	/**
	 * Callback that a stage has finished loading.
	 * <br><b>NOTE: Currently unimplemented.</b>
	 * @param stage the Stage that has finished.
	 */
	public void stageComplete(Stage stage) {
	}

	/**
	 * Callback that a stage has failed. Used to inform user that an error occurred.
	 * @param stage the Stage that was loading when the error occurred
	 */
	public void stageFailed(Stage stage) {
		stop();
		displayError("Connection Error", "Starting " + stage.getName() + " failed");
	}

	/**
	 * Callback that the connection has finished loading and is started.
	 */
	public void connectionStarted() {
		streamFrame.hideSpinner();

		gamepad = new GamepadHandler(conn);
		GamepadListener.getInstance().addDeviceListener(gamepad);
	}

	/**
	 * Callback that the connection has been terminated for some reason.
	 * <br>This is were the stream shutdown procedure takes place.
	 * @param e the Exception that was thrown- probable cause of termination.
	 */
	public void connectionTerminated(Exception e) {
		if (!(e instanceof InterruptedException)) {
			e.printStackTrace();
		}
		if (!connectionTerminating) {
			stop();

			// Spin off a new thread to update the UI since
			// this thread has been interrupted and will terminate
			// shortly
			new Thread(new Runnable() {
				public void run() {
					displayError("Connection Terminated", "The connection failed unexpectedly");
				}
			}).start();
		}
	}

	public static String pair(final String uniqueId, final String host) {
		String message = "";

		NvHTTP httpConn;
		try {
			httpConn = new NvHTTP(InetAddress.getByName(host),
					uniqueId, PlatformBinding.getDeviceName(), PlatformBinding.getCryptoProvider());
			try {
				String serverInfo = httpConn.getServerInfo(uniqueId);
				if (httpConn.getPairState(serverInfo) == PairingManager.PairState.PAIRED) {
					message = "Already paired";
				}
				else if (httpConn.getCurrentGame(serverInfo) != 0) {
					message = "Computer is currently in a game. You must close the game before pairing.";
				}
				else {
					final String pinStr = PairingManager.generatePinString();
					
					// Spin the dialog off in a thread because it blocks
					new Thread(new Runnable() {
						public void run() {
							Limelight.displayUiMessage(null, "Please enter the following PIN on the target PC: "+pinStr,
									"Moonlight", JOptionPane.INFORMATION_MESSAGE);
						}
					}).start();
					
					PairingManager.PairState pairState = httpConn.pair(pinStr);
					if (pairState == PairingManager.PairState.PIN_WRONG) {
						message = "Incorrect PIN";
					}
					else if (pairState == PairingManager.PairState.FAILED) {
						message = "Pairing failed";
					}
					else if (pairState == PairingManager.PairState.PAIRED) {
						message = "Paired successfully";
					}
				}
			} catch (Exception e) {
				message = e.getMessage();
				e.printStackTrace();
			}
		} catch (UnknownHostException e1) {
			message = "Failed to resolve host";
		}
		
		// Dismiss the pairing dialog before showing the final status dialog
		JOptionPane.getRootFrame().dispose();

		return message;

	}

	/**
	 * Displays a message to the user
	 * @param message the message to show the user
	 */
	public void displayMessage(String message) {
		streamFrame.dispose();
		displayUiMessage(limeFrame, message, "Moonlight", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Displays an error to the user
	 * @param title the title for the dialog frame
	 * @param message the message to show the user
	 */
	public void displayError(String title, String message) {
		streamFrame.dispose();
		displayUiMessage(limeFrame, message, title, JOptionPane.ERROR_MESSAGE);
	}

	public void displayTransientMessage(String message) {
		// FIXME: Implement transient GUI messages
		LimeLog.info(message);
	}
}

