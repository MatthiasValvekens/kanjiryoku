package be.mapariensis.kanjiryoku.net.input;

import be.mapariensis.kanjiryoku.model.InputMethod;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerSubmissionException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public interface InputHandler {
    void receiveMessage(String user, NetworkMessage msg)
            throws ServerCommunicationException;

    void broadcastClearInput() throws ServerSubmissionException;

    void clearLocalInput();

    InputMethod inputType();

    void submit() throws ServerSubmissionException;

    void prepareProblemPosition(Problem p, int position);
}
