package com.limelight;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

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
import com.limelight.settings.SettingsManager;
import com.limelight.settings.PreferencesManager.Preferences;
import com.limelight.settings.PreferencesManager.Preferences.Resolution;

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
		streamFrame.build(this, conn, streamConfig, prefs);
		conn.start(PlatformBinding.getDeviceName(), streamFrame,
				VideoDecoderRenderer.FLAG_PREFER_QUALITY,
				PlatformBinding.getAudioRenderer(),
				decoderRenderer);
	}

	/*
	 * Creates a StreamConfiguration given a Resolution. 
	 * Used to specify what kind of stream will be used.
	 */
	public static StreamConfiguration createConfiguration(Resolution res, Integer bitRate, String appName, boolean localAudio) {
		return new StreamConfiguration.Builder()
		.setApp(new NvApp(appName))
		.setResolution(res.width, res.height)
		.setRefreshRate(res.frameRate)
		.setBitrate(bitRate*1000)
		.enableLocalAudioPlayback(localAudio)
		.build();
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
		StreamConfiguration streamConfig = createConfiguration(prefs.getResolution(), prefs.getBitrate(), appName, prefs.getLocalAudio());

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
			}
		}
		
		// Native look and feel for all platforms
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
		}

		//fix the menu bar if we are running in osx
		if (System.getProperty("os.name").contains("Mac OS X")) {
			// set the name of the application menu item (doesn't work on newer Oracle JDK)
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Moonlight");
			// enables the osx-style menu bar
			System.setProperty("apple.laf.useScreenMenuBar", "true");
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

	//TODO: make this less jank
	private static void parseCommandLine(String[] args) {
		String host = null;
		boolean fullscreen = false;
		boolean localAudio = false;
		int resolution = 720;
		int refresh = 60;
		Integer bitrate = null;
		String appName = "Steam";
		
		Preferences prefs = PreferencesManager.getPreferences();
		
		// Save preferences to preserve possibly new unique ID
		PreferencesManager.writePreferences(prefs);

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-pair")) {
				if (i + 1 < args.length){
					host = args[i+1];
					System.out.println("Trying to pair to: " + host);
					String msg = pair(prefs.getUniqueId(), host);
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
			} else if (args[i].equals("-bitrate")) {
				if (i + 1 < args.length){
					bitrate = Integer.parseInt(args[i+1]);
					i++;
				} else {
					System.err.println("Syntax error: bitrate (in Mbps) expected after -bitrate");
					System.exit(3);
				}
			} else if (args[i].equals("-app")) {
				if (i + 1 < args.length){
					appName = args[i+1];
					i++;
				} else {
					System.err.println("Syntax error: app name expected after -app");
					System.exit(3);
				}
			} else if (args[i].equals("-fs")) {
				fullscreen = true;
			} else if (args[i].equals("-la")) {
				localAudio = true;
			} else if (args[i].equals("-720")) {
				resolution = 720;
			} else if (args[i].equals("-768")) {
			    resolution = 768;
			} else if (args[i].equals("-900")) {
			    resolution = 900;
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

		Resolution streamRes = Resolution.findRes(resolution, refresh);
		
		if (bitrate == null) {
			bitrate = streamRes.defaultBitrate;
		}

		StreamConfiguration streamConfig = createConfiguration(streamRes, bitrate, appName, localAudio);
		
		prefs.setResolution(streamRes);
		prefs.setBitrate(bitrate);
		prefs.setFullscreen(fullscreen);
		prefs.setLocalAudio(localAudio);
		
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
				String serverInfo = httpConn.getServerInfo();
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

