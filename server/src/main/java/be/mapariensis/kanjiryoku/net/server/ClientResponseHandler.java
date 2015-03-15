package be.mapariensis.kanjiryoku.net.server;

import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.ResponseHandler;
import be.mapariensis.kanjiryoku.net.model.User;

public abstract class ClientResponseHandler extends ResponseHandler {

	public abstract void handle(User user, NetworkMessage msg)
			throws ServerException;

}
