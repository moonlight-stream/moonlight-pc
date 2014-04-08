package com.limelight.gui;

import com.limelight.LimeLog;
import com.limelight.Limelight;
import com.limelight.input.KeyboardHandler;
import com.limelight.input.MouseHandler;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.NvConnectionListener.Stage;
import com.limelight.nvstream.StreamConfiguration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * The frame to which the video is rendered
 *
 * @author Diego Waxemberg
 *         <br>Cameron Gutman
 */
public class StreamFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final double DESIRED_ASPECT_RATIO   = 16.0 / 9.0;
    private static final double ALTERNATE_ASPECT_RATIO = 16.0 / 10.0;

    private static Cursor noCursor = Toolkit.getDefaultToolkit()
                                            .createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
                                                                new Point(0, 0),
                                                                "blank cursor");

    private KeyboardHandler keyboard;
    private MouseHandler    mouse;
    private JProgressBar    spinner;
    private JLabel          spinnerLabel;
    private Limelight       limelight;
    private JPanel          renderingSurface;

    /**
     * Frees the mouse ie. makes it visible and allowed to move outside the frame.
     */
    public void freeMouse() {
        mouse.free();
        showCursor();
    }

    /**
     * Captures the mouse ie. makes it invisible and not allowed to leave the frame
     */
    public void captureMouse() {
        mouse.capture();
        hideCursor();
    }

    /**
     * Builds the components of this frame with the specified configurations.
     *
     * @param conn         the connection this frame belongs to
     * @param streamConfig the configurations for this frame
     * @param fullscreen   if the frame should be made fullscreen
     */
    public void build(Limelight limelight, NvConnection conn, StreamConfiguration streamConfig, boolean fullscreen) {
        this.limelight = limelight;

        keyboard = new KeyboardHandler(conn, this);
        mouse = new MouseHandler(conn, this);

        this.setBackground(Color.BLACK);
        this.setFocusableWindowState(true);
        this.setFocusTraversalKeysEnabled(false);
        this.addWindowListener(createWindowListener());

        Container contentPane = this.getContentPane();

        renderingSurface = new JPanel(false);
        renderingSurface.addKeyListener(keyboard);
        renderingSurface.addMouseListener(mouse);
        renderingSurface.addMouseMotionListener(mouse);
        renderingSurface.setBackground(Color.BLACK);
        renderingSurface.setIgnoreRepaint(true);
        renderingSurface.setFocusable(true);
        renderingSurface.setLayout(new BoxLayout(renderingSurface, BoxLayout.Y_AXIS));
        renderingSurface.setVisible(true);

        contentPane.setLayout(new BorderLayout());
        contentPane.add(renderingSurface, "Center");

        if (fullscreen) {
            makeFullScreen(streamConfig);

            // OS X hack for full-screen losing focus
            if (System.getProperty("os.name").contains("Mac OS X")) {
                this.setVisible(false);
                this.setVisible(true);
            }
        } else {
            this.setVisible(true);

            // Only fill the available screen area (excluding taskbar, etc)
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
            Insets windowInsets = this.getInsets();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            int windowInsetWidth = windowInsets.left + windowInsets.right;
            int windowInsetHeight = windowInsets.top + windowInsets.bottom;
            int maxWidth = screenSize.width - (screenInsets.left + screenInsets.right);
            int maxHeight = screenSize.height - (screenInsets.top + screenInsets.bottom);
            this.setSize(new Dimension(Math.min(streamConfig.getWidth() + windowInsetWidth, maxWidth),
                                       Math.min(streamConfig.getHeight() + windowInsetHeight, maxHeight)));
        }

        hideCursor();
    }

    private ArrayList<DisplayMode> getDisplayModesByAspectRatio(DisplayMode[] configs, double aspectRatio) {
        ArrayList<DisplayMode> matchingConfigs = new ArrayList<DisplayMode>();

        for (DisplayMode config : configs) {
            if ((double) config.getWidth() / (double) config.getHeight() == aspectRatio) {
                matchingConfigs.add(config);
            }
        }

        return matchingConfigs;
    }

    private DisplayMode getBestDisplay(StreamConfiguration targetConfig, DisplayMode[] configs) {
        int targetDisplaySize = targetConfig.getWidth() * targetConfig.getHeight();

        // Try to match the target aspect ratio
        ArrayList<DisplayMode> aspectMatchingConfigs = getDisplayModesByAspectRatio(configs, DESIRED_ASPECT_RATIO);
        if (aspectMatchingConfigs.size() == 0) {
            // No matches for the target, so try the alternate
            aspectMatchingConfigs = getDisplayModesByAspectRatio(configs, ALTERNATE_ASPECT_RATIO);
            if (aspectMatchingConfigs.size() == 0) {
                // No matches for either, so just use all of them
                aspectMatchingConfigs = new ArrayList<DisplayMode>(Arrays.asList(configs));
            }
        }

        // Sort by display size
        Collections.sort(aspectMatchingConfigs, new Comparator<DisplayMode>() {
            public int compare(DisplayMode o1, DisplayMode o2) {
                if (o1.getWidth() * o1.getHeight() > o2.getWidth() * o2.getHeight()) {
                    return -1;
                } else if (o2.getWidth() * o2.getHeight() > o1.getWidth() * o1.getHeight()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        // Find the aspect-matching config with the closest matching display size
        DisplayMode bestConfig = null;
        for (DisplayMode config : aspectMatchingConfigs) {
            if (config.getWidth() * config.getHeight() >= targetDisplaySize) {
                bestConfig = config;
            }
        }

        if (bestConfig != null) {
            LimeLog.info("Using full-screen display mode " + bestConfig.getWidth() + "x" + bestConfig.getHeight() +
                         " for " + targetConfig.getWidth() + "x" + targetConfig.getHeight() + " stream");
        } else {
            bestConfig = aspectMatchingConfigs.get(0);
            LimeLog.info("No matching display modes. Using largest: "
                         + bestConfig.getWidth()
                         + "x"
                         + bestConfig.getHeight()
                         +
                         " for "
                         + targetConfig.getWidth()
                         + "x"
                         + targetConfig.getHeight()
                         + " stream");
        }

        return bestConfig;
    }

    private void makeFullScreen(StreamConfiguration streamConfig) {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            this.setResizable(false);
            this.setUndecorated(true);
            gd.setFullScreenWindow(this);

            if (gd.isDisplayChangeSupported()) {
                DisplayMode config = getBestDisplay(streamConfig, gd.getDisplayModes());
                if (config != null) {
                    gd.setDisplayMode(config);
                }
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Unable to change display resolution. \nThis may not be the correct resolution",
                        "Display Resolution",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Your operating system does not support fullscreen.",
                    "Fullscreen Unsupported",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Makes the mouse cursor invisible
     */
    public void hideCursor() {
        hideCursor(this);
    }


    /**
     * Makes the mouse cursor invisible
     */
    public static void hideCursor(Component component) {
        // Set the blank cursor to the JFrame.
        component.setCursor(noCursor);
    }

    /**
     * Makes the mouse cursor visible
     */
    public void showCursor() {
        showCursor(this);
    }

    /**
     * Makes the mouse cursor visible
     */
    public static void showCursor(Component component) {
        component.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Shows a progress bar with a label underneath that tells the user what
     * loading stage the stream is at.
     *
     * @param stage the currently loading stage
     */
    public void showSpinner(Stage stage) {
        if (spinner == null) {
            spinner = new JProgressBar();
            spinner.setIndeterminate(true);
            spinner.setMaximumSize(new Dimension(150, 30));

            spinnerLabel = new JLabel();
            spinnerLabel.setForeground(Color.white);

            Box spinBox = Box.createHorizontalBox();
            spinBox.add(Box.createHorizontalGlue());
            spinBox.add(spinner);
            spinBox.add(Box.createHorizontalGlue());

            Box lblBox = Box.createHorizontalBox();
            lblBox.add(Box.createHorizontalGlue());
            lblBox.add(spinnerLabel);
            lblBox.add(Box.createHorizontalGlue());

            renderingSurface.add(Box.createVerticalGlue());
            renderingSurface.add(spinBox);
            renderingSurface.add(Box.createVerticalStrut(10));
            renderingSurface.add(lblBox);
            renderingSurface.add(Box.createVerticalGlue());
        }
        spinnerLabel.setText("Starting " + stage.getName() + "...");
    }

    /**
     * Creates the listener for the window.
     * It terminates the connection when the window is closed
     */
    private WindowListener createWindowListener() {
        return new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        };
    }

    /**
     * Hides the spinner and the label
     */
    public void hideSpinner() {
        renderingSurface.removeAll();
        renderingSurface.requestFocus();
    }

    /**
     * Stops the stream and destroys the frame
     */
    public void close() {
        limelight.stop();
        dispose();
        if (Limelight.COMMAND_LINE_LAUNCH) {
            System.exit(0);
        }
    }

    public JPanel getRenderingSurface() {
        return renderingSurface;
    }
}