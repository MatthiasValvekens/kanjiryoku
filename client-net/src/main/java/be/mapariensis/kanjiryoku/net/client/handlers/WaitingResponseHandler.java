package be.mapariensis.kanjiryoku.net.client.handlers;

import be.mapariensis.kanjiryoku.net.client.ServerResponseHandler;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

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
