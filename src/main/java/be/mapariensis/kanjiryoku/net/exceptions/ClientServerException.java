package be.mapariensis.kanjiryoku.net.exceptions;

import java.text.ParseException;
import java.util.regex.Pattern;

import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class ClientServerException extends Exception {
	public static final int ERROR_GENERIC = 0;
	public static final int ERROR_USER_MANAGEMENT = 1;
	public static final int ERROR_SESSION_ISSUE = 2;
	public static final int ERROR_IO = 3;
	public static final int ERROR_SYNTAX = 4;
	public static final int ERROR_QUEUE = 5;
	public static final int ERROR_NOT_SUPPORTED = 6;
	public static final int ERROR_BACKEND = 6;
	public static final int ERROR_GAME_INTERNAL = 7;
	public static final int ERROR_SERVER_COMM = 8;
	
	public final int errorCode;
	public ClientServerException(String message, int errorCode) {
		this(message,null,errorCode);
	}

	public ClientServerException(Throwable cause, int errorCode) {
		this(cause==null ? null : cause.getMessage(),cause,errorCode);
	}

	public ClientServerException(String message, Throwable cause, int errorCode) {
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
	private static final Pattern errorPattern = Pattern.compile("E\\d\\d\\d");
	public static boolean isError(String message) {
		return errorPattern.matcher(message).matches();
	}
}
