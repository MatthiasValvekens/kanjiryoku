package be.mapariensis.kanjiryoku.net.input;

import be.mapariensis.kanjiryoku.net.exceptions.ServerSubmissionException;

public interface MultipleChoiceInputHandler extends InputHandler {
    void broadcastSelect(int choice) throws ServerSubmissionException;
}
