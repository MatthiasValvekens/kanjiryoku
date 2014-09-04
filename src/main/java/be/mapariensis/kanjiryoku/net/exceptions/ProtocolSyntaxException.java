package be.mapariensis.kanjiryoku.net.exceptions;

public class ProtocolSyntaxException extends ServerException {
	public ProtocolSyntaxException() {
		super("Syntax Error",ERROR_SYNTAX);
	}
	public ProtocolSyntaxException(String message) {
		super(message, ERROR_SYNTAX);
	}

	public ProtocolSyntaxException(String message, Throwable cause) {
		super(message, cause, ERROR_SYNTAX);
	}

	public ProtocolSyntaxException(Throwable cause) {
		super(cause.toString(),cause, ERROR_SYNTAX);
	}

}
