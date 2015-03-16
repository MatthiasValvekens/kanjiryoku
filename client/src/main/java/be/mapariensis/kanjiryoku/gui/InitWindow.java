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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.Kanjiryoku;
import be.mapariensis.kanjiryoku.config.ConfigManager;
import be.mapariensis.kanjiryoku.config.ConfigManager.ConfigListener;
import be.mapariensis.kanjiryoku.gui.dialogs.CreateConnectionDialog;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.profiles.ProfileSet;
import be.mapariensis.kanjiryoku.net.profiles.ProfileSet.Profile;
import be.mapariensis.kanjiryoku.util.IProperties;

public class InitWindow {
	private static final Logger log = LoggerFactory.getLogger(InitWindow.class);

	public static final String PROFILE_SET_KEY = "profiles";

	private final JFrame frame;
	private final ProfileSet profiles;
	private final JButton editButton, deleteButton, connectButton;

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

	private InitWindow(final ConfigManager manager)
			throws BadConfigurationException {
		frame = new JFrame("Connect to Kanjiryoku server");
		frame.setIconImage(Kanjiryoku.ICON);

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
		frame.add(new JLabel("Select server"), gbc);

		gbc.gridheight = 3;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(0, 10, 10, 10);
		gbc.gridy++;
		final JList<String> profileList = new JList<String>(profiles);
		profileList.setPrototypeCellValue("AAAAAAAAAAAAAAAAAAAAAAA");
		profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		frame.add(new JScrollPane(profileList), gbc);

		Action connectAction = new AbstractAction("Connect") {

			@Override
			public void actionPerformed(ActionEvent e) {
				Profile p = profiles.get(profileList.getSelectedValue());
				if (p == null)
					return;
				try {
					MainWindow wind = new MainWindow(
							InetAddress.getByName(p.host), p.port, p.username,
							p.useSsl ? manager.getSSLContext(p.host) : null);
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
		Action addAction = new AbstractAction("Add") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new CreateConnectionDialog(frame, profiles).setVisible(true);
			}
		};
		Action editAction = new AbstractAction("Edit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selection = profileList.getSelectedValue();
				if (selection == null)
					return;
				new CreateConnectionDialog(frame, profiles, selection)
						.setVisible(true);
			}
		};
		Action deleteAction = new AbstractAction("Delete") {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selection = profileList.getSelectedValue();
				if (selection == null)
					return;
				int response = JOptionPane.showConfirmDialog(frame,
						"Are you sure?", "Delete", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE);
				if (response == JOptionPane.YES_OPTION)
					profiles.removeProfile(selection);
			}
		};
		GridBagConstraints buttonConstraints = new GridBagConstraints();
		buttonConstraints.gridx = 0;
		buttonConstraints.gridy = 4;
		buttonConstraints.fill = GridBagConstraints.HORIZONTAL;
		buttonConstraints.gridwidth = 2;
		buttonConstraints.weightx = 0.5;
		buttonConstraints.insets = new Insets(5, 10, 10, 10);
		frame.add(connectButton = new JButton(connectAction), buttonConstraints);

		// button panel
		JPanel buttonBox = new JPanel(new GridBagLayout());
		buttonConstraints.gridx = 0;
		buttonConstraints.gridy = 0;
		buttonConstraints.fill = GridBagConstraints.HORIZONTAL;
		buttonConstraints.insets = new Insets(1, 10, 1, 10);
		buttonBox.add(new JButton(addAction), buttonConstraints);
		buttonConstraints.gridy++;
		buttonBox.add(editButton = new JButton(editAction), buttonConstraints);
		buttonConstraints.gridy++;
		buttonBox.add(deleteButton = new JButton(deleteAction),
				buttonConstraints);

		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 1;
		frame.add(buttonBox, gbc);

		// not available unless something is selected
		editButton.setEnabled(false);
		deleteButton.setEnabled(false);
		connectButton.setEnabled(false);
		profileList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (profileList.getSelectedValue() != null) {
					editButton.setEnabled(true);
					deleteButton.setEnabled(true);
					connectButton.setEnabled(true);
				}
			}
		});

		frame.pack();
	}

	public static void show(ConfigManager manager)
			throws BadConfigurationException {
		new InitWindow(manager).frame.setVisible(true);
	}
}
