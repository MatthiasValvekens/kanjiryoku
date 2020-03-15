package be.mapariensis.kanjiryoku.gui.dialogs;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.client.ServerUplink;
import be.mapariensis.kanjiryoku.net.client.handlers.WaitingResponseHandler;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class CreateSessionDialog extends NetworkedDialog {
	private static final StringPair dummyElement = new StringPair(null,
			"AAAAAAAAAAAAAAAAAAAAAAA");

	private static class StringPair {
		final String name;
		final String humanName;

		public StringPair(String name, String humanName) {
			this.name = name;
			this.humanName = humanName;
		}

		@Override
		public String toString() {
			return humanName;
		}
	}

	private final DefaultListModel<StringPair> gameListModel = new DefaultListModel<>();
	private final DefaultListModel<String> userListModel = new DefaultListModel<>();
	private final JList<StringPair> gameList = new JList<>(
			gameListModel);

	public CreateSessionDialog(Frame parent, ServerUplink serv) {
		super(parent, "Create session", "Create a new session", serv);
		// dummy to compute size

		JPanel panel = getContents();
		panel.setLayout(new GridBagLayout());

		// game selector
		GridBagConstraints listLabelConstraints = new GridBagConstraints();
		listLabelConstraints.anchor = GridBagConstraints.LINE_END;
		listLabelConstraints.gridx = 0;
		listLabelConstraints.gridy = 0;
		listLabelConstraints.gridwidth = 2;
		listLabelConstraints.weightx = 0.5;
		panel.add(new JLabel("Game type"), listLabelConstraints);
		gameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		gameList.setLayoutOrientation(JList.VERTICAL);
		gameList.setVisibleRowCount(3);
		gameList.setPrototypeCellValue(dummyElement);
		GridBagConstraints listBodyConstraints = new GridBagConstraints();
		listBodyConstraints.anchor = GridBagConstraints.LINE_START;
		listBodyConstraints.gridx = 2;
		listBodyConstraints.gridy = 0;
		listBodyConstraints.gridheight = 3;
		listBodyConstraints.gridwidth = 2;
		listBodyConstraints.weightx = 0.5;
		listBodyConstraints.insets = new Insets(0, 10, 0, 10);
		panel.add(new JScrollPane(gameList,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
				listBodyConstraints);

		// user selector
		JList<String> userList = new JList<>(userListModel);
		userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		userList.setLayoutOrientation(JList.VERTICAL);
		userList.setVisibleRowCount(3);
		userList.setPrototypeCellValue(dummyElement.humanName);
		listLabelConstraints.gridy = 3;
		panel.add(new JLabel("Invited users"), listLabelConstraints);
		listBodyConstraints.gridy = 3;
		panel.add(new JScrollPane(userList,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
				listBodyConstraints);
		listLabelConstraints.gridy = 6;
		panel.add(new JLabel("Add user"), listLabelConstraints);
		listBodyConstraints.gridy = 6;
		final JTextField inviteField = new JTextField(
				dummyElement.humanName.length());
		panel.add(inviteField, listBodyConstraints);
		inviteField.addActionListener(e -> {
			userListModel.addElement(inviteField.getText());
			inviteField.setText("");
		});

	}

	@Override
	protected boolean verifyInput() {
		return gameList.getSelectedIndex() != -1;
	}

	@Override
	protected NetworkMessage constructMessage() {
		StringPair thing = gameList.getSelectedValue();
		List<Object> args = new ArrayList<>(userListModel.getSize() + 2);
		args.add(ServerCommandList.STARTSESSION);
		args.add(thing.name);
		for (int i = 0; i < userListModel.getSize(); i++) {
			args.add(userListModel.get(i));
		}
		return new NetworkMessage(args);
	}

	@Override
	protected void setUp() throws ClientException {
		WaitingResponseHandler wrh = new WaitingResponseHandler();
		NetworkMessage msg = getServer().blockUntilResponse(
				new NetworkMessage(ServerCommandList.LISTGAMES, wrh.id), wrh,
				Constants.STANDARD_MACRO_TIMEOUT);
		if (msg.argCount() != 3)
			throw new ServerCommunicationException(msg);
		final JSONArray gameJSON = new JSONArray(msg.get(2));
		for (int i = 0; i < gameJSON.length(); i++) {
			JSONObject obj = gameJSON.getJSONObject(i);
			gameListModel.addElement(new StringPair(obj
					.getString(Constants.GAMELIST_JSON_NAME), obj
					.getString(Constants.GAMELIST_JSON_HUMANNAME)));
		}
	}

}
