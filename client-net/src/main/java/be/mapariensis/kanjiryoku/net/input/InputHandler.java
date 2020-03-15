package be.mapariensis.kanjiryoku.net.input;

import be.mapariensis.kanjiryoku.model.InputMethod;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public interface InputHandler {
	void receiveMessage(String user, NetworkMessage msg)
			throws ServerCommunicationException;

	void broadcastClearInput();

	void clearLocalInput();

	InputMethod inputType();

	void submit();

	void prepareProblemPosition(Problem p, int position);
}
