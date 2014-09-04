package be.mapariensis.kanjiryoku.net.exceptions;

public class ServerBackendException extends ServerException {
	public ServerBackendException(Throwable cause) {
		super(cause,ERROR_BACKEND);
	}
}
