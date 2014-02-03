package com.limelight.gui;

import com.limelight.Limelight;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.settings.PreferencesManager;
import org.xmlpull.v1.XmlPullParserException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: spartango
 * Date: 2/2/14
 * Time: 3:02 PM.
 */
public class AppsFrame extends JFrame {
    // Connection to the host
    private NvHTTP httpConnection;
    private String host;

    private Map<String, NvApp> apps;

    // UI Elements
    private JComboBox appSelector;
    private JButton   launchButton;

    public AppsFrame(NvHTTP httpConnection,
                     String host) {
        super("Apps");
        this.httpConnection = httpConnection;
        this.host = host;

        apps = new HashMap<String, NvApp>();
    }

    /**
     * Constructs all components of the frame and makes the frame visible to the user.
     */
    public void build() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        appSelector = new JComboBox();
        Collection<NvApp> fetched = fetchApps();
        for (NvApp app : fetched) {
            apps.put(app.getAppName(), app);
            appSelector.addItem(app.getAppName());
        }

        launchButton = new JButton("Launch");
        launchButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String appName = (String) appSelector.getSelectedItem();
                NvApp app = apps.get(appName);
                if (app != null) {
                    launchApp(app);
                }
            }
        });

        Box appSelectorBox = Box.createHorizontalBox();
        appSelectorBox.add(Box.createHorizontalGlue());
        appSelectorBox.add(appSelector);
        appSelectorBox.add(Box.createHorizontalGlue());

        Box launchBox = Box.createHorizontalBox();
        launchBox.add(Box.createHorizontalGlue());
        launchBox.add(launchButton);
        launchBox.add(Box.createHorizontalGlue());

        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(appSelectorBox);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(launchBox);
        mainPanel.add(Box.createVerticalGlue());

        this.getContentPane().add(mainPanel);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        //center on screen
        this.setLocation((int) dim.getWidth() / 2 - this.getWidth() / 2,
                         (int) dim.getHeight() / 2 - this.getHeight() / 2);

        if (!apps.isEmpty()) {
            this.setVisible(true);
            this.setSize(200, 100);
            this.setResizable(false);
        }
    }

    private Collection<NvApp> fetchApps() {
        // List out the games that are installed
        try {
            if (httpConnection.getCurrentGame() != 0) {
                httpConnection.resumeApp();
                System.out.println("Resumed existing game session");
            } else {
                return httpConnection.getAppList();
            }
        } catch (IOException e) {
            // If any of that fails, fallback to launching steam big picture
            System.err.println("Failed to fetch app list: " + e);
        } catch (XmlPullParserException e) {
            System.err.println("Failed to parse app list: " + e);
        }

        launchSteam();
        return Collections.EMPTY_LIST;
    }

    private void launchApp(NvApp app) {
        // Use the http connection to launch the app remotely
        PreferencesManager.Preferences prefs = PreferencesManager.getPreferences();
        StreamConfiguration config = Limelight.createConfiguration(prefs.getResolution());
        int gameSessionId = 0;
        try {
            gameSessionId = httpConnection.launchApp(app.getAppId(), config.getWidth(),
                                                     config.getHeight(), config.getRefreshRate());
            if (gameSessionId != 0) {
                System.out.println("Launched new game session");        // Let the NV Connection reconnect appropriately, establishing the streaming
                launchSteam();
            }
        } catch (IOException e) {
            System.err.println("Failed to launch app " + e);
        } catch (XmlPullParserException e) {
            System.err.println("Failed to launch app: " + e);
        }
    }

    private void launchSteam() {
        this.setVisible(false);
        // Start the stream
        Limelight.createInstance(host);
    }

}
