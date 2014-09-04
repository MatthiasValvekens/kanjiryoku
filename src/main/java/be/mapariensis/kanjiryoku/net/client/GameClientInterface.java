package be.mapariensis.kanjiryoku.net.client;

import be.mapariensis.kanjiryoku.net.model.NetworkMessage;


public interface GameClientInterface {
	public void submit(NetworkMessage msg);
}
