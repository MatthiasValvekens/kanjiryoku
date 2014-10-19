package be.mapariensis.kanjiryoku.gui.dialogs;

import java.awt.Frame;

import be.mapariensis.kanjiryoku.gui.ChatInterface;
import be.mapariensis.kanjiryoku.net.client.ServerUplink;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class PrivateMessageDialog extends SingleInputDialog {
	private final String username;
	private final String me;
	private final ChatInterface chat;
	private String message;
	private long time;
	public PrivateMessageDialog(Frame parent, ServerUplink serv, String username, ChatInterface chat) {
		super(parent, "Send private message", String.format("To %s:",username), serv,true);
		this.username = username;
		this.chat = chat;
		this.me = serv.getUsername();
	}


	@Override
	protected NetworkMessage constructMessage() {
		message = getInput();
		NetworkMessage res = new NetworkMessage(ServerCommandList.MESSAGE,username,message);
		time = res.timestamp;
		return res;
	}
	
	@Override
	protected void tearDown() {
		chat.displayUserMessage(time, me, String.format("(To %s) %s",username,message),false); // TODO : Is this the right design?
	}

}
