package be.mapariensis.kanjiryoku.gui;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

import be.mapariensis.kanjiryoku.net.client.ServerResponseHandler;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.util.History;
import be.mapariensis.kanjiryoku.util.StandardHistoryImpl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
// TODO enforce timestamp ordering
public class ChatPanel extends JPanel implements ChatInterface {
	private final JTextArea textpanel = new JTextArea();
	private static final String HISTORY_BACK = "history_back";
	private static final String HISTORY_FORWARD = "history_forward";
	public static final int HISTORY_SIZE=20;
	private final Executor promptThreads = Executors.newSingleThreadExecutor(); // ensure only one prompt can exist at a time
	private final GUIBridge bridge;
	private final History history = new StandardHistoryImpl(HISTORY_SIZE);
	private final ServerResponseHandler dumpToChat = new ServerResponseHandler() {

		@Override
		public void handle(NetworkMessage msg)	throws ClientException {
			displayServerMessage(msg.toString(2));
		}
		
	};
	
	public ChatPanel(final GUIBridge bridge) {
		this.bridge = bridge;
		setPreferredSize(new Dimension(400,600));
		setLayout(new BorderLayout());
		textpanel.setEditable(false);
		textpanel.setLineWrap(true);
		textpanel.setFont(new Font("Serif",Font.PLAIN,13));
		DefaultCaret caret = (DefaultCaret)textpanel.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		add(new JScrollPane(textpanel),BorderLayout.CENTER);
		add(new JLabel("Chat"),BorderLayout.NORTH);
		JPanel controls = new JPanel();
		final JTextField input = new JTextField(20);
		controls.add(input);
		input.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String msg = input.getText();
				history.add(msg);
				if(msg.isEmpty()) return;
				if(msg.charAt(0)==':') {
					bridge.getUplink().enqueueMessage(NetworkMessage.buildArgs(msg.substring(1))); // interpret the rest as a command
					input.setText("");
					return;
				}
				displayUserMessage(bridge.getUplink().getUsername(),msg);
				bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.SESSIONMESSAGE, msg));
				input.setText("");
			}
		});
		input.getActionMap().put(HISTORY_BACK, new AbstractAction(HISTORY_BACK) {

			@Override
			public void actionPerformed(ActionEvent e) {
				input.setText(history.back());
			}
			
		});
		input.getActionMap().put(HISTORY_FORWARD,new AbstractAction(HISTORY_FORWARD) {

			@Override
			public void actionPerformed(ActionEvent e) {
				input.setText(history.forward());
			}
			
		});
		input.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), HISTORY_BACK);
		input.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), HISTORY_FORWARD);
		add(controls,BorderLayout.SOUTH);	
	}

	@Override
	public void displayServerMessage(String message) {
		synchronized(textpanel) {
			textpanel.append(String.format("*server*\t%s\n",message));
		}
		
	}
	@Override
	public void displayUserMessage(String from, String message) {
		synchronized(textpanel) {
			textpanel.append(String.format("[%s]\t%s\n",from,message));
		}
	}
	@Override
	public void displayErrorMessage(int errorId, String message) {
		synchronized(textpanel) {
			textpanel.append(String.format("Error E%03d\t%s\n",errorId,message));
		}
	}
	private class YesNoTask implements Runnable {
		final String question;
		final NetworkMessage ifNo, ifYes;
		public YesNoTask(String question, NetworkMessage ifYes,
				NetworkMessage ifNo) {
			this.question = question;
			this.ifNo = ifNo;
			this.ifYes = ifYes;
		}

		@Override
		public void run() {
			int res = JOptionPane.showConfirmDialog(ChatPanel.this, question,"Server prompt",JOptionPane.YES_NO_OPTION);
			bridge.getUplink().enqueueMessage(res == JOptionPane.YES_OPTION ? ifYes : ifNo);
		}
		
	}
	@Override
	public void yesNoPrompt(String question, NetworkMessage ifYes,
			NetworkMessage ifNo) {
		promptThreads.execute(new YesNoTask(question, ifYes,ifNo));
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
		synchronized(textpanel) {
			textpanel.append(String.format("*system*\t%s\n",message));
		}
	}
}