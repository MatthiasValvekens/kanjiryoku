package be.mapariensis.kanjiryoku.net.exceptions;

public class CommandQueueingException extends ServerException {

	public CommandQueueingException(String message) {
		super(message, ERROR_QUEUE);
	}

	public CommandQueueingException(String message, Throwable cause) {
		super(message, cause, ERROR_QUEUE);
	}

	public CommandQueueingException(Throwable cause) {
		super(cause, ERROR_QUEUE);
	}

}
