package be.mapariensis.kanjiryoku.net.client;

import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.ServerResponseHandler;

public interface ChatInterface {
	public void displayServerMessage(String message);
	public void displayUserMessage(String from, String message);
	public void displayErrorMessage(int errorId, String message);
	public void displayErrorMessage(ClientServerException ex);
	public void displaySystemMessage(String message);
	public void yesNoPrompt(String question, NetworkMessage ifYes, NetworkMessage ifNo);
	public ServerResponseHandler getDefaultResponseHandler();
	
}
