package be.mapariensis.kanjiryoku.net.exceptions;

// TODO server error codes
public class SessionException extends ServerException {

    public SessionException(String message, Throwable cause) {
        super(message, cause, ERROR_SESSION_ISSUE);
    }

    public SessionException(String message) {
        super(message, ERROR_SESSION_ISSUE);
    }

    public SessionException(Throwable cause) {
        super(cause, ERROR_SESSION_ISSUE);
    }

}
