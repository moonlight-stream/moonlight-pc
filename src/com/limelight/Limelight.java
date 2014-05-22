package com.limelight;


import com.limelight.binding.LibraryHelper;
import com.limelight.binding.PlatformBinding;
import com.limelight.gui.MainFrame;
import com.limelight.gui.StreamFrame;
import com.limelight.input.gamepad.Gamepad;
import com.limelight.input.gamepad.GamepadListener;
import com.limelight.input.gamepad.NativeGamepad;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.NvConnectionListener;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.settings.PreferencesManager;
import com.limelight.settings.SettingsManager;
import org.xmlpull.v1.XmlPullParserException;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static com.limelight.settings.PreferencesManager.Preferences;
import static com.limelight.settings.PreferencesManager.Preferences.Resolution;

/**
 * Main class for Limelight-pc contains methods for starting the application as well
 * as the stream to the host pc.
 *
 * @author Diego Waxemberg<br>
 *         Cameron Gutman
 */
public class Limelight implements NvConnectionListener {
	public static final double VERSION = 1.0;
	public static boolean COMMAND_LINE_LAUNCH = false;

	private String host;
	private StreamFrame streamFrame;
	private NvConnection conn;
	private boolean connectionTerminating;
	private static JFrame limeFrame;
	private Gamepad gamepad;

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
	private void startUp(StreamConfiguration streamConfig, boolean fullscreen) {
		streamFrame = new StreamFrame();

		conn = new NvConnection(host, this, streamConfig);
		streamFrame.build(this, conn, streamConfig, fullscreen);
		conn.start(PlatformBinding.getDeviceName(), streamFrame,
				VideoDecoderRenderer.FLAG_PREFER_QUALITY,
				PlatformBinding.getAudioRenderer(),
				PlatformBinding.getVideoDecoderRenderer());
	}

	/*
	 * Creates a StreamConfiguration given a Resolution.
	 * Used to specify what kind of stream will be used.
	 */
	private static StreamConfiguration createConfiguration(Resolution res) {
		switch(res) {
		case RES_720_30:
			return new StreamConfiguration(1280, 720, 30, 5000);
		case RES_720_60:
			return new StreamConfiguration(1280, 720, 60, 10000);
		case RES_1080_30:
			return new StreamConfiguration(1920, 1080, 30, 10000);
		case RES_1080_60:
			return new StreamConfiguration(1920, 1080, 60, 25000);
		default:
			// this should never happen, if it does we want the NPE to occur so we know something is wrong
			return null;
		}
	}

	/*
	 * Creates the main frame for the application.
	 */
	private static void createFrame() {
		// Tell the user how to map the gamepad if it's a new install and there's no default for this platform
		if (!PreferencesManager.hasExistingPreferences() &&
				!System.getProperty("os.name").contains("Windows")) {
			JOptionPane.showMessageDialog(null, "Gamepad mapping is not set. If you want to use a gamepad, "+
					"click the Options menu and choose Gamepad Settings. After mapping your gamepad,"+
					" it will work while streaming.", "Limelight", JOptionPane.INFORMATION_MESSAGE);
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
	public static void createInstance(String host) {
		Limelight limelight = new Limelight(host);

		Preferences prefs = PreferencesManager.getPreferences();
		StreamConfiguration streamConfig = createConfiguration(prefs.getResolution());

		limelight.startUp(streamConfig, prefs.getFullscreen());
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
			}
		}

		//fix the menu bar if we are running in osx
		if (System.getProperty("os.name").contains("Mac OS X")) {
			// set the name of the application menu item
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Limelight");
		}

		GamepadListener.getInstance().addDeviceListener(new Gamepad());

		String libraryError = loadNativeLibraries();

		// launching with command line arguments
		if (args.length > 0) {
			if (libraryError == null) {
				parseCommandLine(args);
			}
			else {
				// Print the error to stderr if running from command line
				System.err.println(libraryError);
			}
		} else {
			if (libraryError == null) {
				createFrame();
			} else {
				JOptionPane.showMessageDialog(null, libraryError, "Wrong JAR platform", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	//TODO: make this less jank
	private static void parseCommandLine(String[] args) {
		String host = null;
		boolean fullscreen = false;
		int resolution = 720;
		int refresh = 60;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-pair")) {
				if (i + 1 < args.length){
					host = args[i+1];
					System.out.println("Trying to pair to: " + host);
					String msg = pair(host);
					System.out.println("Pairing: " + msg);
					System.exit(0);
				} else {
					System.err.println("Syntax error: hostname or ip address expected after -pair");
					System.exit(4);
				}
			} else if (args[i].equals("-host")) {
				if (i + 1 < args.length) {
					host = args[i+1];
					i++;
				} else {
					System.err.println("Syntax error: hostname or ip address expected after -host");
					System.exit(3);
				}
			} else if (args[i].equals("-fs")) {
				fullscreen = true;
			} else if (args[i].equals("-720")) {
				resolution = 720;
			} else if (args[i].equals("-1080")) {
				resolution = 1080;
			} else if (args[i].equals("-30fps")) {
				refresh = 30;
			} else if (args[i].equals("-60fps")) {
				refresh = 60;
			} else {
				System.out.println("Syntax Error: Unrecognized argument: " + args[i]);
			}
		}

		if (host == null) {
			System.out.println("Syntax Error: You must include a host. Use -host to specifiy a hostname or ip address.");
			System.exit(5);
		}

		Resolution streamRes = null;

		if (resolution == 720 && refresh == 30) {
			streamRes = Resolution.RES_720_30;
		} else if (resolution == 720 && refresh == 60) {
			streamRes = Resolution.RES_720_60;
		} else if (resolution == 1080 && refresh == 30) {
			streamRes = Resolution.RES_1080_30;
		} else if (resolution == 1080 && refresh == 60) {
			streamRes = Resolution.RES_1080_60;
		}

		StreamConfiguration streamConfig = createConfiguration(streamRes);

		Limelight limelight = new Limelight(host);
		limelight.startUp(streamConfig, fullscreen);
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

		gamepad = new Gamepad(conn);
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

	public static String pair(final String host) {
		String message = "";
		String macAddress;
		try {
			macAddress = NvConnection.getMacAddressString();
		} catch (SocketException e) {
			e.printStackTrace();
			message = "An error occured trying to get this system's MAC address";
			return message;
		}

		if (macAddress == null) {
			message = "Couldn't find a MAC address";
			LimeLog.severe(message);
			return message;
		}

		NvHTTP httpConn;
		try {
			httpConn = new NvHTTP(InetAddress.getByName(host),
					macAddress, PlatformBinding.getDeviceName());
			try {
				if (httpConn.getPairState()) {
					message = "Already paired";
				}
				else {
					int session = httpConn.getSessionId();
					if (session == 0) {
						message = "Pairing was declined by the target";
					}
					else {
						message = "Pairing was successful";
					}
				}
			} catch (IOException e) {
				message = e.getMessage();
			} catch (XmlPullParserException e) {
				message = e.getMessage();
			}
		} catch (UnknownHostException e1) {
			message = "Failed to resolve host";
		}

		return message;

	}

	/**
	 * Displays a message to the user in the form of an info dialog.
	 * @param message the message to show the user
	 */
	public void displayMessage(String message) {
		streamFrame.dispose();
		JOptionPane.showMessageDialog(limeFrame, message, "Limelight", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Displays an error to the user in the form of an error dialog
	 * @param title the title for the dialog frame
	 * @param message the message to show the user
	 */
	public void displayError(String title, String message) {
		streamFrame.dispose();
		JOptionPane.showMessageDialog(limeFrame, message, title, JOptionPane.ERROR_MESSAGE);
	}

	public void displayTransientMessage(String message) {
		// FIXME: Implement transient messages
	}
}

