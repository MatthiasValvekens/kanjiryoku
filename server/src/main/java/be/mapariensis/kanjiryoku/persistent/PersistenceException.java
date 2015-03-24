package be.mapariensis.kanjiryoku.persistent;

import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;

public class PersistenceException extends ServerBackendException {

	public PersistenceException(String message) {
		super(message);
	}

	public PersistenceException(Throwable cause) {
		super(cause);
	}

}
