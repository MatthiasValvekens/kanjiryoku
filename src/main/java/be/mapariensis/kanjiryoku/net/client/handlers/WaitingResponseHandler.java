package be.mapariensis.kanjiryoku.net.client.handlers;

import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.ServerResponseHandler;

public class WaitingResponseHandler extends ServerResponseHandler {
	private volatile NetworkMessage msg;
	public NetworkMessage getMessage() {
		return msg;
	}
	@Override
	public void handle(NetworkMessage msg) {
		this.msg = msg;
	}

}
