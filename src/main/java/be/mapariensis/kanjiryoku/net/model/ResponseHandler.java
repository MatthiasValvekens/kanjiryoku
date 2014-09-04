package be.mapariensis.kanjiryoku.net.model;

import be.mapariensis.kanjiryoku.net.exceptions.ServerException;



public interface ResponseHandler {
	/**
	 * Handle a client response. This method is guaranteed to be called by only one thread at a time.
	 * @param user
	 * @param msg
	 * @throws ServerException
	 */
	public void handle(User user, NetworkMessage msg) throws ServerException;
}
