package be.mapariensis.kanjiryoku.net.exceptions;

import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class ServerSubmissionException extends ClientException {

    public ServerSubmissionException(String message) {
        super(message, ERROR_IO);
    }

    public ServerSubmissionException(NetworkMessage msg, Exception ex) {
        super("Error while submitting data to server: " + msg, ex, ERROR_IO);
    }
}
