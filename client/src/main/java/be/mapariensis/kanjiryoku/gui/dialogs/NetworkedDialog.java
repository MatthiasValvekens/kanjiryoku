package be.mapariensis.kanjiryoku.gui.dialogs;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;
import javax.swing.border.EtchedBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.client.ServerUplink;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public abstract class NetworkedDialog extends JDialog {
	private static final Logger log = LoggerFactory
			.getLogger(NetworkedDialog.class);
	private static final String setupString = "Setting up...";
	private static final String teardownString = "Cleaning up...";
	private static final String errorString = "Communication error";
	private final JButton submitButton;
	private final JPanel contentPanel = new JPanel();
	private final ServerUplink serv;
	private final JLabel statusLabel = new JLabel("");
	private NetworkMessage message;
	private final Action submitAction;

	protected NetworkedDialog(final Frame parent, String title,
			String headerText, ServerUplink serv) {
		super(parent, title, true);
		this.serv = serv;
		JPanel contentPanelWrapper = new JPanel();
		contentPanelWrapper.setBorder(BorderFactory
				.createEtchedBorder(EtchedBorder.LOWERED));
		contentPanelWrapper.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(10, 10, 10, 10);
		contentPanelWrapper.add(contentPanel, c);
		setLayout(new GridBagLayout());
		// content panel
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.gridheight = 2;
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.5;
		c.insets = new Insets(10, 10, 10, 10);
		add(contentPanelWrapper, c);

		// header label
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 1;
		c.gridy = 0;
		add(new JLabel(headerText), c);

		// set up submit button
		submitAction = new AbstractAction("Submit") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!verifyInput()) {
					JOptionPane.showMessageDialog(parent,
							"Input is invalid or incomplete", "Invalid input",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				message = constructMessage();
				NetworkedDialog.this.serv.enqueueMessage(message);
				statusLabel.setText(teardownString);
				try {
					tearDown();
					statusLabel.setText("");
				} catch (Exception ex) {
					log.error("Exception in dialog", ex);
					statusLabel.setText(errorString);
				}
				setVisible(false);
			}
		};
		submitButton = new JButton(submitAction);
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 1;
		c.gridy = 4;
		c.weightx = 0.5;
		add(submitButton, c);

		// status label
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 1;
		c.gridy = 5;
		c.weightx = 0.5;
		c.insets = new Insets(10, 0, 0, 0);
		add(statusLabel, c);

		// set up when window opens
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent ev) {
				statusLabel.setText(setupString);
				try {
					pack();
					setUp();
					pack();
					statusLabel.setText("");
				} catch (Exception e) {
					log.error("Exception in dialog", e);
					statusLabel.setText(errorString);
				}
			}

		});
		setResizable(false);
	}

	protected final ServerUplink getServer() {
		return serv;
	}

	public JPanel getContents() {
		return contentPanel;
	}

	public final void removeItem(Component c) {
		contentPanel.remove(c);
	}

	protected abstract boolean verifyInput();

	protected abstract NetworkMessage constructMessage();

	/**
	 * Called on WINDOW_OPEN. Do communication with server here.
	 * 
	 * @throws ClientException
	 */
	protected void setUp() throws ClientException {

	}

	/**
	 * Called after the message constructed by constructMessage() has been sent.
	 * 
	 * @throws ClientException
	 */
	protected void tearDown() throws ClientException {

	}

	protected Action getSubmitAction() {
		return submitAction;
	}
}
