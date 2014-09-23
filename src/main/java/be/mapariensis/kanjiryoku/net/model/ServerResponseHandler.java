package be.mapariensis.kanjiryoku.net.model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.exceptions.ClientException;

public abstract class ServerResponseHandler extends ResponseHandler {
	private final Logger log = LoggerFactory.getLogger(ServerResponseHandler.class);
	@Override
	public final void handle(User user, NetworkMessage msg)
			throws ClientException {
		if(user != null)
			log.warn("Non-null user parameter  passed to server response handler. Ignoring.");
		handle(msg);
	}
	
	public abstract void handle(NetworkMessage msg) throws ClientException;

}
