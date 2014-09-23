package be.mapariensis.kanjiryoku.net.model;

import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;



public abstract class ResponseHandler {
	public static final int DEFAULT_HANDLER_ID = -1;
	public final int id;
	protected ResponseHandler() {
		// add an ID to the response handler based on the current time
		// this is a "soft" check to prevent the client from accidentally responding to the wrong command
		// if the queueing mechanism goes bananas
		this.id = (int) (System.currentTimeMillis() % 10000);
	}

	/**
	 * Handle a client response. This method is guaranteed to be called by only one thread at a time.
	 * @param user
	 * @param msg
	 * @throws ServerException
	 */
	public abstract void handle(User user, NetworkMessage msg) throws ClientServerException;
}
