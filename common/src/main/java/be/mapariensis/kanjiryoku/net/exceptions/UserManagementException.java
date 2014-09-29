package be.mapariensis.kanjiryoku.net.exceptions;

public class UserManagementException extends ServerException {

	public UserManagementException(String message, Throwable cause) {
		super(message, cause, ERROR_USER_MANAGEMENT);
	}

	public UserManagementException(String message) {
		super(message, ERROR_USER_MANAGEMENT);
	}

	public UserManagementException(Throwable cause) {
		super(cause, ERROR_USER_MANAGEMENT);
	}

}
