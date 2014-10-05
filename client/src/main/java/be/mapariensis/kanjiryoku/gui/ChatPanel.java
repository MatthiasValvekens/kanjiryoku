package be.mapariensis.kanjiryoku.gui;

import javax.swing.*;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.util.History;
import be.mapariensis.kanjiryoku.util.StandardHistoryImpl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
// TODO enforce timestamp ordering
public class ChatPanel extends JPanel {
	
	private static final String HISTORY_BACK = "history_back";
	private static final String HISTORY_FORWARD = "history_forward";
	public static final int HISTORY_SIZE=20;
	
	private final History history = new StandardHistoryImpl(HISTORY_SIZE);
	private boolean firstManualCommandSent = false;
	
	public ChatPanel(final GUIBridge bridge, JComponent chatRenderer) {
		setPreferredSize(new Dimension(400,600));
		setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(chatRenderer);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane,BorderLayout.CENTER);
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
				if(msg.charAt(0)=='\\') {
					if(!firstManualCommandSent) { 
						bridge.getChat().displaySystemMessage("Lines starting with '\\' are interpreted as server commands.");
						firstManualCommandSent = true;
					}
					bridge.getUplink().enqueueMessage(NetworkMessage.buildArgs(msg.substring(1))); // interpret the rest as a command
					input.setText("");
					return;
				}
				bridge.getChat().displayUserMessage(bridge.getUplink().getUsername(),msg);
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

}