package be.mapariensis.kanjiryoku.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigManager;
import be.mapariensis.kanjiryoku.config.ConfigManager.ConfigListener;
import be.mapariensis.kanjiryoku.gui.dialogs.CreateConnectionDialog;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.profiles.ProfileSet;
import be.mapariensis.kanjiryoku.net.profiles.ProfileSet.Profile;
import be.mapariensis.kanjiryoku.util.IProperties;

public class InitWindow {
	private static final Logger log = LoggerFactory.getLogger(InitWindow.class);
	private final JFrame frame = new JFrame("Connect to Kanjiryoku server");
	private final ProfileSet profiles;
	public static final String PROFILE_SET_KEY = "profiles";

	private class ConfigListenerProxy implements ListDataListener {
		private final ConfigListener<JSONObject> l;

		ConfigListenerProxy(ConfigListener<JSONObject> l) {
			this.l = l;
		}

		@Override
		public void intervalAdded(ListDataEvent e) {
			contentsChanged(e);
		}

		@Override
		public void intervalRemoved(ListDataEvent e) {
			contentsChanged(e);
		}

		@Override
		public void contentsChanged(ListDataEvent e) {
			l.changed(profiles.toJSON());

		}

	}

	private InitWindow(ConfigManager manager) throws BadConfigurationException {
		IProperties profileProperties = manager.getCurrentConfig().getTyped(
				PROFILE_SET_KEY, IProperties.class);
		if (profileProperties != null) {
			this.profiles = new ProfileSet(profileProperties);
		} else {
			this.profiles = new ProfileSet();
		}
		// instruct config manager to record changes made to profiles
		ConfigListener<JSONObject> listener = manager.watch(PROFILE_SET_KEY);
		this.profiles.addListDataListener(new ConfigListenerProxy(listener));

		frame.setLayout(new GridBagLayout());
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(0, 0, 0, 10);
		gbc.weightx = 0.5;
		frame.add(new JLabel("Connection profile"), gbc);

		gbc.gridheight = 3;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(0, 10, 10, 10);
		gbc.gridy++;
		final JList<String> profileList = new JList<String>(profiles);
		profileList.setPrototypeCellValue("AAAAAAAAAAAAAAAAAAAAAAA");
		profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		frame.add(new JScrollPane(profileList), gbc);

		GridBagConstraints buttonConstraints = new GridBagConstraints();
		buttonConstraints.gridx = 0;
		buttonConstraints.gridy = 4;
		buttonConstraints.fill = GridBagConstraints.HORIZONTAL;
		buttonConstraints.gridwidth = 2;
		buttonConstraints.weightx = 0.5;
		buttonConstraints.insets = new Insets(10, 10, 10, 10);
		Action connectAction = new AbstractAction("Connect") {

			@Override
			public void actionPerformed(ActionEvent e) {
				Profile p = profiles.get(profileList.getSelectedValue());
				if (p == null)
					return;
				try {
					MainWindow wind = new MainWindow(
							InetAddress.getByName(p.host), p.port, p.username);
					frame.setVisible(false);
					frame.dispose();
					wind.setVisible(true);
				} catch (UnknownHostException ex) {
					JOptionPane.showMessageDialog(frame,
							String.format("Unknown host \"%s\".", p.host),
							"Connection error", JOptionPane.ERROR_MESSAGE);
					log.warn(ex.getMessage(), ex);
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(frame,
							"I/O error during connect.", "Connection error",
							JOptionPane.ERROR_MESSAGE);
					log.warn(ex.getMessage(), ex);
				}
			}
		};
		frame.add(new JButton(connectAction), buttonConstraints);
		Action addAction = new AbstractAction("Add") {

			@Override
			public void actionPerformed(ActionEvent e) {
				new CreateConnectionDialog(frame, profiles).setVisible(true);
			}
		};
		buttonConstraints.gridx = 2;
		buttonConstraints.gridy = 1;
		buttonConstraints.fill = GridBagConstraints.NONE;
		frame.add(new JButton(addAction), buttonConstraints);

		frame.pack();
	}

	public static void show(ConfigManager manager)
			throws BadConfigurationException {
		new InitWindow(manager).frame.setVisible(true);
	}
}
