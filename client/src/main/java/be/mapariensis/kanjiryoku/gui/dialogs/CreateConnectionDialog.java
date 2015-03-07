package be.mapariensis.kanjiryoku.gui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.profiles.ProfileSet;
import be.mapariensis.kanjiryoku.net.profiles.ProfileSet.Profile;

public class CreateConnectionDialog extends JDialog {
	private static final Logger log = LoggerFactory
			.getLogger(CreateConnectionDialog.class);

	private static final int FIELD_LEN = 20;

	public CreateConnectionDialog(JFrame parent, final ProfileSet profiles) {
		this(parent, profiles, null);
	}

	public CreateConnectionDialog(JFrame parent, final ProfileSet profiles,
			final String initialProfile) {
		super(parent, "Register connection profile", true);
		setLayout(new GridBagLayout());
		setResizable(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = 0;
		labelConstraints.insets = new Insets(3, 3, 3, 10);
		labelConstraints.weightx = 0.5;
		labelConstraints.anchor = GridBagConstraints.LINE_END;

		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridx = 1;
		fieldConstraints.gridy = 0;
		fieldConstraints.weightx = 0.5;
		fieldConstraints.gridwidth = 2;
		fieldConstraints.insets = new Insets(3, 0, 3, 10);
		put(new JLabel("Name:"), labelConstraints);
		final JTextField profileName = new JTextField(FIELD_LEN);
		put(profileName, fieldConstraints);

		// add separator
		GridBagConstraints sepConstraints = new GridBagConstraints();
		sepConstraints.gridx = 0;
		sepConstraints.gridy = labelConstraints.gridy;
		sepConstraints.fill = GridBagConstraints.HORIZONTAL;
		sepConstraints.weighty = 1;
		sepConstraints.gridwidth = 2;
		add(new JSeparator(JSeparator.HORIZONTAL), sepConstraints);

		fieldConstraints.gridy++;
		labelConstraints.gridy++;
		put(new JLabel("Hostname:"), labelConstraints);
		final JTextField hostname = new JTextField(FIELD_LEN);
		put(hostname, fieldConstraints);

		put(new JLabel("Port:"), labelConstraints);
		final JTextField port = new JTextField(FIELD_LEN);
		put(port, fieldConstraints);

		put(new JLabel("Username:"), labelConstraints);
		final JTextField username = new JTextField(FIELD_LEN);
		put(username, fieldConstraints);

		GridBagConstraints buttonConstraints = new GridBagConstraints();
		buttonConstraints.gridx = 1;
		buttonConstraints.gridy = labelConstraints.gridy;
		buttonConstraints.weightx = 0.5;
		buttonConstraints.insets = new Insets(10, 0, 0, 0);
		Action addAction = new AbstractAction("Confirm") {

			@Override
			public void actionPerformed(ActionEvent e) {
				String host = hostname.getText();
				String user = username.getText();
				String profile = profileName.getText();
				try {
					if (user.isEmpty() || profile.isEmpty())
						throw new RuntimeException("Required field empty");
					profiles.replaceProfile(initialProfile, profile, host,
							Integer.valueOf(port.getText()), user);
					setVisible(false);
					dispose();
				} catch (RuntimeException ex) {
					JOptionPane.showMessageDialog(CreateConnectionDialog.this,
							"Illegal values in input.", "Invalid input",
							JOptionPane.ERROR_MESSAGE);
					log.warn(ex.getMessage(), ex);
				}
			}
		};
		add(new JButton(addAction), buttonConstraints);
		username.addActionListener(addAction);

		// initialize fields when appropriate
		if (initialProfile != null) {
			Profile p = profiles.get(initialProfile);
			if (p != null) {
				profileName.setText(initialProfile);
				hostname.setText(p.host);
				port.setText(Integer.toString(p.port));
				username.setText(p.username);
			}
		}
		pack();
	}

	private void put(JComponent comp, GridBagConstraints c) {
		add(comp, c);
		c.gridy++;
	}
}
