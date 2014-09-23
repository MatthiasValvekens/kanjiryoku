package be.mapariensis.kanjiryoku.net.model;

import be.mapariensis.kanjiryoku.net.exceptions.ServerException;

public abstract class ClientResponseHandler extends ResponseHandler {

	@Override
	public abstract void handle(User user, NetworkMessage msg)
			throws ServerException;

}
