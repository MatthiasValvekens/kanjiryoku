package be.mapariensis.kanjiryoku.gui;

import be.mapariensis.kanjiryoku.net.client.ServerUplink;

public interface UIBridge {
	ServerUplink getUplink();

	GameClientInterface getClient();

	ChatInterface getChat();

	void setUsername(String username);

	String promptPassword();

	void close();
}
