package be.mapariensis.kanjiryoku.gui;

import be.mapariensis.kanjiryoku.net.client.ServerResponseHandler;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public interface ChatInterface {
	public void displayServerMessage(long timestamp, String message);
	public void displayGameMessage(long timestamp, String message);
	public void displayUserMessage(long timestamp, String from, String message, boolean broadcast);
	public void displayErrorMessage(int errorId, String message);
	public void displayErrorMessage(ClientServerException ex);
	public void displaySystemMessage(String message);
	public void yesNoPrompt(String question, NetworkMessage ifYes, NetworkMessage ifNo);
	public ServerResponseHandler getDefaultResponseHandler();
	
}
