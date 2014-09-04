package be.mapariensis.kanjiryoku.net.exceptions;

import java.text.ParseException;

import be.mapariensis.kanjiryoku.net.model.NetworkMessage;


public class ServerException extends Exception {
	public static final int ERROR_GENERIC = 0;
	public static final int ERROR_USER_MANAGEMENT = 1;
	public static final int ERROR_SESSION_ISSUE = 2;
	public static final int ERROR_IO = 3;
	public static final int ERROR_SYNTAX = 4;
	public static final int ERROR_QUEUE = 5;
	
	public final int errorCode;
	public ServerException(String message, int errorCode) {
		this(message,null,errorCode);
	}

	public ServerException(Throwable cause, int errorCode) {
		this(cause==null ? null : cause.getMessage(),cause,errorCode);
	}

	public ServerException(String message, Throwable cause, int errorCode) {
		super(wrap(message,errorCode), cause);
		this.errorCode = errorCode;
	}
	// make sure the message is protocol-compliant
	private static String wrap(String message, int errorCode) {
		return new StringBuilder().append(formatErrorCode(errorCode)).append(" ").append(NetworkMessage.escapedAtom(message)).toString();
	}
	
	public static String formatErrorCode(int errorCode) {
		return String.format("E%03d", errorCode);
	}
	public static int parseErrorCode(String in) throws ParseException {
		try {
			return Integer.valueOf(in.substring(1));
		} catch (NumberFormatException ex) {
			throw new ParseException("Invalid error code",1);
		}
	}
}
