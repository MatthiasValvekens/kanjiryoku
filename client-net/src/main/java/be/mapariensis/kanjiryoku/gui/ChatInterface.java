package be.mapariensis.kanjiryoku.gui;

import be.mapariensis.kanjiryoku.net.client.ServerResponseHandler;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerSubmissionException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public interface ChatInterface {
	void displayServerMessage(long timestamp, String message);

	void displayGameMessage(long timestamp, String message);

	void displayUserMessage(long timestamp, String from, String message,
			boolean broadcast);

	void displayErrorMessage(int errorId, String message);

	void displayErrorMessage(ClientServerException ex);

	void displaySystemMessage(String message);

	void yesNoPrompt(String question, NetworkMessage ifYes,
			NetworkMessage ifNo) throws ServerSubmissionException;

	ServerResponseHandler getDefaultResponseHandler();

}
