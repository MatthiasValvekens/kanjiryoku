package be.mapariensis.kanjiryoku.gui;

import be.mapariensis.kanjiryoku.net.client.ServerUplink;

public interface UIBridge {
	public ServerUplink getUplink();

	public GameClientInterface getClient();

	public ChatInterface getChat();

	public void setUsername(String username);

	public String promptPassword();

	public void close();
}
