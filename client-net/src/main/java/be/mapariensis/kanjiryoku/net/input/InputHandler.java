package be.mapariensis.kanjiryoku.net.input;

import be.mapariensis.kanjiryoku.model.InputMethod;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public interface InputHandler {
	public void receiveMessage(String user, NetworkMessage msg)
			throws ServerCommunicationException;

	public void broadcastClearInput();

	public void clearLocalInput();

	public InputMethod inputType();

	public void submit();

	public void prepareProblemPosition(Problem p, int position);
}
