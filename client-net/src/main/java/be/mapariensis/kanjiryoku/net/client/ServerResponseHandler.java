package be.mapariensis.kanjiryoku.net.client;

import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.ResponseHandler;

public abstract class ServerResponseHandler extends ResponseHandler {

	public abstract void handle(NetworkMessage msg) throws ClientException;

}
