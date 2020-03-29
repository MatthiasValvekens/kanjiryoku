package be.mapariensis.kanjiryoku.net.exceptions;

public class ServerBackendException extends ServerException {
    public ServerBackendException(Throwable cause) {
        super("Internal server error. Page the admin.", cause, ERROR_BACKEND);
    }

    public ServerBackendException(String message) {
        super("Internal server error. Page the admin.", new RuntimeException(
                message), ERROR_BACKEND);
    }
}
