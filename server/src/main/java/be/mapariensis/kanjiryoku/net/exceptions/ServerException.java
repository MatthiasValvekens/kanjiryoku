package be.mapariensis.kanjiryoku.net.exceptions;

public class ServerException extends ClientServerException {

	public ServerException(String message, int errorCode) {
		super(message, errorCode);
	}

	public ServerException(String message, Throwable cause, int errorCode) {
		super(message, cause, errorCode);
	}

	public ServerException(Throwable cause, int errorCode) {
		super(cause, errorCode);
	}

}
