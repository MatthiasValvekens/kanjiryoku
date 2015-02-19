package be.mapariensis.kanjiryoku.net.exceptions;

public class BadConfigurationException extends ClientServerException {
	public BadConfigurationException(String message) {
		super(message, ERROR_SERVER_CONFIG);
	}

	public BadConfigurationException(Throwable cause) {
		super(cause, ERROR_SERVER_CONFIG);
	}

	public BadConfigurationException(String message, Throwable cause) {
		super(message, cause, ERROR_SERVER_CONFIG);
	}
}
