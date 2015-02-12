package com.limelight.gui;

import com.limelight.LimeLog;
import com.limelight.Limelight;
import com.limelight.binding.PlatformBinding;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

    public AppsFrame(String host) {
        super("Apps");
        this.host = host;

        apps = new HashMap<String, NvApp>();
    }

    /**
     * Constructs all components of the frame and makes the frame visible to the user.
     */
    public void build() {
        try {
			this.httpConnection = new NvHTTP(InetAddress.getByName(host),
					PreferencesManager.getPreferences().getUniqueId(),
					PlatformBinding.getDeviceName(), PlatformBinding.getCryptoProvider());
		} catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, "Unable to resolve machine address",
					"Limelight", JOptionPane.ERROR_MESSAGE);
			return;
		}
    	
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        appSelector = new JComboBox();
        appSelector.addItem("Loading apps...");

        // Send this to be done asynchronously
        SwingWorker<LinkedList<NvApp>, Void> fetchBg = new SwingWorker<LinkedList<NvApp>, Void>() {
            @Override protected LinkedList<NvApp> doInBackground() throws Exception {
                return fetchApps();
            }

            @Override protected void done() {
                try {
                    LinkedList<NvApp> fetched = get();
                    if (fetched == null) {
                    	return;
                    }
                    
                    // Sort the fetched list alphabetically by app name
                    Collections.sort(fetched, new Comparator<NvApp>() {
						@Override
						public int compare(NvApp left, NvApp right) {
							return left.getAppName().compareTo(right.getAppName());
						}
                    });
                    
                    appSelector.removeAllItems();
                    
                    for (NvApp app : fetched) {
                        apps.put(app.getAppName(), app);
                        appSelector.addItem(app.getAppName());
                    }
                    
                    for (NvApp app : fetched) {
                    	if (app.getIsRunning()) {
                            appSelector.setSelectedItem(app.getAppName());
                    	}
                    }
                } catch (InterruptedException e) {
                    LimeLog.warning("Failed to get list of apps; interrupted by " + e);
                } catch (ExecutionException e) {
                    LimeLog.warning("Failed to get list of apps; broken by " + e);
                }
            }
        };
        fetchBg.execute();

        launchButton = new JButton("Launch");
        launchButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String appName = (String) appSelector.getSelectedItem();
                NvApp app = apps.get(appName);
                if (app != null) {
                    launchApp(app.getAppName());
                } else {
                    launchApp("Steam");
                }
            }
        });
        getRootPane().setDefaultButton(launchButton);

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

        this.setVisible(true);
        this.setSize(225, 115);
        this.setResizable(false);
    }

    private LinkedList<NvApp> fetchApps() {
        // List out the games that are installed
        try {
            return httpConnection.getAppList();
        } catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Unable to retreive app list",
					"Limelight", JOptionPane.ERROR_MESSAGE);
			setVisible(false);
        }
        
        return null;
    }

    private void launchApp(String appName) {
        this.setVisible(false);
        Limelight.createInstance(host, appName);
    }
}
