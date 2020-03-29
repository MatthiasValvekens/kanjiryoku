package be.mapariensis.kanjiryoku.net.exceptions;

public class ClientException extends ClientServerException {

    public ClientException(String message, int errorCode) {
        super(message, errorCode);
    }

    public ClientException(String message, Throwable cause, int errorCode) {
        super(message, cause, errorCode);
    }

    public ClientException(Throwable cause, int errorCode) {
        super(cause, errorCode);
    }

}
