package be.mapariensis.kanjiryoku.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import be.mapariensis.kanjiryoku.Constants;
import be.mapariensis.kanjiryoku.gui.utils.DummyResponseHandler;
import be.mapariensis.kanjiryoku.gui.utils.YesNoTask;
import be.mapariensis.kanjiryoku.net.client.ServerResponseHandler;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class PlainChatPanel extends JPanel implements ChatInterface {
	private final JTextArea textpanel = new JTextArea();
	private final Executor promptThreads = Executors.newSingleThreadExecutor(); // ensure
																				// only
																				// one
																				// prompt
																				// can
																				// exist
																				// at
																				// a
																				// time
	private final ServerResponseHandler dumpToChat = new DummyResponseHandler(
			this);
	private final UIBridge bridge;

	public PlainChatPanel(UIBridge bridge) {
		this.bridge = bridge;
		textpanel.setEditable(false);
		textpanel.setLineWrap(true);
		textpanel.setFont(new Font("Serif", Font.PLAIN, 13));
		DefaultCaret caret = (DefaultCaret) textpanel.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		setLayout(new BorderLayout());
		add(textpanel, BorderLayout.CENTER);
	}

	@Override
	public void displayServerMessage(long timestamp, String message) {
		synchronized (textpanel) {
			textpanel.append(String.format("%s\t%s\n", Constants.SERVER_HANDLE,
					message));
		}

	}

	@Override
	public void displayUserMessage(long timestamp, String from, String message,
			boolean broadcast) {
		synchronized (textpanel) {
			textpanel.append(String.format("[%s]\t%s\n", from, message));
		}
	}

	@Override
	public void displayErrorMessage(int errorId, String message) {
		synchronized (textpanel) {
			textpanel.append(String.format("Error E%03d\t%s\n", errorId,
					message));
		}
	}

	@Override
	public void yesNoPrompt(String question, NetworkMessage ifYes,
			NetworkMessage ifNo) {
		promptThreads.execute(new YesNoTask(this, bridge.getUplink(), question,
				ifYes, ifNo));
	}

	@Override
	public void displayErrorMessage(ClientServerException ex) {
		displayErrorMessage(ex.errorCode, ex.getMessage());
	}

	@Override
	public ServerResponseHandler getDefaultResponseHandler() {
		return dumpToChat;
	}

	@Override
	public void displaySystemMessage(String message) {
		synchronized (textpanel) {
			textpanel.append(String.format("*system*\t%s\n", message));
		}
	}

	@Override
	public void displayGameMessage(long timestamp, String message) {
		synchronized (textpanel) {
			textpanel.append(String.format("*game*\t%s\n", message));
		}
	}
}
