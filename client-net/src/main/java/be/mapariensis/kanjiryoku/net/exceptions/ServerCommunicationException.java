package be.mapariensis.kanjiryoku.net.exceptions;

import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class ServerCommunicationException extends ClientException {
	public ServerCommunicationException(String message) {
		super(message, ERROR_SERVER_COMM);
	}

	public ServerCommunicationException(NetworkMessage msg) {
		this("Error while processing data from server: " + msg);
	}

	public ServerCommunicationException(Exception ex) {
		super("Exception raised while processing server data", ex,
				ERROR_GENERIC);
	}
	public ServerCommunicationException(NetworkMessage msg, Exception ex) {
		super("Exception raised while processing server data: "+ msg, ex,
				ERROR_GENERIC);
	}
}
