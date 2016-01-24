package com.limelight.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.limelight.LimeLog;
import com.limelight.Limelight;
import com.limelight.nvstream.mdns.MdnsComputer;
import com.limelight.nvstream.mdns.MdnsDiscoveryAgent;
import com.limelight.nvstream.mdns.MdnsDiscoveryListener;
import com.limelight.settings.PreferencesManager;
import com.limelight.settings.PreferencesManager.Preferences;

import javax.swing.*;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.util.HashMap;

/**
 * The main frame of Moonlight that allows the user to specify the host and begin the stream.
 *
 * @author Diego Waxemberg
 *         <br>Cameron Gutman
 */
public class MainFrame {
	private JTextField hostField;
	private JButton pair;
	private JButton stream;
	private JFrame limeFrame;
	private JComboBox<String> mdnsHostList;

    private HashMap<String, InetAddress> mdnsHosts;
    private MdnsDiscoveryAgent mdnsAgent;

	/**
	 * Gets the actual JFrame this class creates
	 * @return the JFrame that is the main frame
	 */
	public JFrame getLimeFrame() {
		return limeFrame;
	}

	/*
	 * Creates the menu bar for the user to go to preferences, mappings, etc.
	 */
	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu optionsMenu = new JMenu("Options");
		JMenuItem gamepadSettings = new JMenuItem("Gamepad Settings");
		JMenuItem generalSettings = new JMenuItem("Preferences");
		generalSettings.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, (Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())));
		
		gamepadSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new GamepadConfigFrame().build();
			}
		});

		generalSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new PreferencesFrame().build();
			}
		});

		optionsMenu.add(gamepadSettings);
		optionsMenu.add(generalSettings);

		menuBar.add(optionsMenu);

		return menuBar;
	}

	/*
	 * Creates the listener for the stream button- starts the stream process
	 */
	private ActionListener createStreamButtonListener() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String host = hostField.getText();
				Preferences prefs = PreferencesManager.getPreferences();
				if (!host.equals(prefs.getHost())) {
					prefs.setHost(host);
					PreferencesManager.writePreferences(prefs);
				}
				// Limelight.createInstance(host);
				showApps();
			}
		};
	}

	/*
	 * Creates the listener for the pair button- requests a pairing with the specified host
	 */
	private ActionListener createPairButtonListener() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					public void run() {
						Preferences prefs = PreferencesManager.getPreferences();
						
						// Save preferences to preserve possibly new unique ID
						PreferencesManager.writePreferences(prefs);
						
						String msg = Limelight.pair(prefs.getUniqueId(), hostField.getText());
						Limelight.displayUiMessage(limeFrame, msg, "Moonlight", JOptionPane.INFORMATION_MESSAGE);
					}
				}).start();
			}
		};
	}

    /**
     * Builds all components of the frame, including the frame itself and displays it to the user.
     */
    public void build() {
        limeFrame = new JFrame("Moonlight");
        limeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container mainPane = limeFrame.getContentPane();

        mainPane.setLayout(new BorderLayout());

        JPanel centerPane = new JPanel();
        centerPane.setLayout(new BoxLayout(centerPane, BoxLayout.Y_AXIS));

        Preferences prefs = PreferencesManager.getPreferences();

        hostField = new JTextField();
        hostField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        hostField.setToolTipText("Enter host name or IP address");
        hostField.setText(prefs.getHost());
        hostField.setSelectionStart(0);
        hostField.setSelectionEnd(hostField.getText().length());

        stream = new JButton("Show App List");
        stream.addActionListener(createStreamButtonListener());

        pair = new JButton("Pair");
        pair.addActionListener(createPairButtonListener());
        pair.setToolTipText("Send pair request to GeForce PC");

        mdnsHosts = new HashMap<String, InetAddress>();
        mdnsHostList = new JComboBox<String>();
        mdnsHostList.addItem("Choose a local PC...");
        
        mdnsAgent = new MdnsDiscoveryAgent(new MdnsDiscoveryListener() {
			@Override
			public void notifyComputerAdded(MdnsComputer computer) {
                if (!mdnsHosts.containsKey(computer.getName())) {
                    mdnsHosts.put(computer.getName(), computer.getAddress());
                    mdnsHostList.addItem(computer.getName());
                }
			}

			@Override
			public void notifyComputerRemoved(MdnsComputer computer) {
                // We'll keep any host we've seen before around, as users will find it convenient
				LimeLog.info("Computer lost: "+computer.getName());
			}

			@Override
			public void notifyDiscoveryFailure(Exception e) {
				LimeLog.warning("Discovery failure");
				e.printStackTrace();
			}
        	
        });
        mdnsAgent.startDiscovery(1000);

        // Propagate selections from mDNS to the hosts field
        mdnsHostList.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED && mdnsHostList.getSelectedIndex() != 0) {
                    hostField.setText((String) mdnsHosts.get(mdnsHostList.getSelectedItem()).getHostAddress());
                }
            }
        });


        Box streamBox = Box.createHorizontalBox();
        streamBox.add(Box.createHorizontalGlue());
        streamBox.add(stream);
        streamBox.add(Box.createHorizontalGlue());

        Box pairBox = Box.createHorizontalBox();
        pairBox.add(Box.createHorizontalGlue());
        pairBox.add(pair);
        pairBox.add(Box.createHorizontalGlue());

        Box hostBox = Box.createHorizontalBox();
        hostBox.add(Box.createHorizontalStrut(20));
        hostBox.add(hostField);
        hostBox.add(Box.createHorizontalStrut(20));

        Box mdnsBox = Box.createHorizontalBox();
        mdnsBox.add(Box.createHorizontalStrut(20));
        mdnsBox.add(mdnsHostList);
        mdnsBox.add(Box.createHorizontalStrut(20));

        Box contentBox = Box.createVerticalBox();
        contentBox.add(Box.createVerticalStrut(20));
        contentBox.add(hostBox);
        contentBox.add(Box.createVerticalStrut(5));
        contentBox.add(mdnsBox);
        contentBox.add(Box.createVerticalStrut(10));
        contentBox.add(streamBox);
        contentBox.add(Box.createVerticalStrut(7));
        contentBox.add(pairBox);
        contentBox.add(Box.createVerticalGlue());

        centerPane.add(contentBox);
        mainPane.add(centerPane, "Center");
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        limeFrame.setJMenuBar(createMenuBar());
        limeFrame.getRootPane().setDefaultButton(stream);
        limeFrame.setSize(300, 230);
        limeFrame.setLocation(dim.width / 2 - limeFrame.getSize().width / 2,
                              dim.height / 2 - limeFrame.getSize().height / 2);
        limeFrame.setResizable(false);
        limeFrame.setVisible(true);
    }

    private void showApps() {
        String host = hostField.getText();
        AppsFrame appsFrame = new AppsFrame(host);
        appsFrame.build();
    }
}
