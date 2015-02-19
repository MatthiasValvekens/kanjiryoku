package be.mapariensis.kanjiryoku.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitWindow {
	private static final int FIELD_LEN = 20;
	private static final Logger log = LoggerFactory.getLogger(InitWindow.class);
	private final JFrame frame = new JFrame("Connect to Kanjiryoku server");

	private InitWindow() {
		frame.setLayout(new GridBagLayout());
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = 0;
		labelConstraints.insets = new Insets(0, 0, 0, 10);
		labelConstraints.weightx = 0.5;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		frame.add(new JLabel("Hostname:"), labelConstraints);
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridx = 1;
		fieldConstraints.gridy = 0;
		fieldConstraints.weightx = 0.5;
		fieldConstraints.gridwidth = 2;
		final JTextField hostname = new JTextField(FIELD_LEN);
		frame.add(hostname, fieldConstraints);
		labelConstraints.gridy = 1;
		frame.add(new JLabel("Port:"), labelConstraints);
		fieldConstraints.gridy = 1;
		final JTextField port = new JTextField(FIELD_LEN);
		frame.add(port, fieldConstraints);
		labelConstraints.gridy = 2;
		frame.add(new JLabel("Username:"), labelConstraints);
		fieldConstraints.gridy = 2;
		final JTextField username = new JTextField(FIELD_LEN);
		frame.add(username, fieldConstraints);
		GridBagConstraints buttonConstraints = new GridBagConstraints();
		buttonConstraints.gridx = 1;
		buttonConstraints.gridy = 3;
		buttonConstraints.weightx = 0.5;
		buttonConstraints.insets = new Insets(10, 0, 0, 0);
		Action connectAction = new AbstractAction("Connect") {

			@Override
			public void actionPerformed(ActionEvent e) {
				String host = hostname.getText();
				String user = username.getText();
				try {
					if (user.isEmpty())
						throw new RuntimeException("User name empty");
					MainWindow wind = new MainWindow(
							InetAddress.getByName(host), Integer.valueOf(port
									.getText()), user);
					frame.setVisible(false);
					frame.dispose();
					wind.setVisible(true);
				} catch (RuntimeException ex) {
					JOptionPane.showMessageDialog(frame,
							"Illegal values in input.", "Invalid input",
							JOptionPane.ERROR_MESSAGE);
					log.warn(ex.getMessage(), ex);
				} catch (UnknownHostException ex) {
					JOptionPane.showMessageDialog(frame,
							String.format("Unknown host \"%s\".", host),
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
		username.addActionListener(connectAction);
		frame.pack();
	}

	public static void show() {
		new InitWindow().frame.setVisible(true);
	}
}
