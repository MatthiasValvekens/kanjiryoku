package be.mapariensis.kanjiryoku.gui.utils;

import be.mapariensis.kanjiryoku.gui.ChatInterface;
import be.mapariensis.kanjiryoku.net.client.ServerResponseHandler;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class DummyResponseHandler extends ServerResponseHandler {
	private final ChatInterface chat;

	public DummyResponseHandler(ChatInterface chat) {
		this.chat = chat;
	}

	@Override
	public void handle(NetworkMessage msg) throws ClientException {
		chat.displayServerMessage(msg.timestamp, msg.toString(2));
	}

}
