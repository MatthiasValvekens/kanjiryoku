package be.mapariensis.kanjiryoku.gui;

import be.mapariensis.kanjiryoku.net.client.ChatInterface;
import be.mapariensis.kanjiryoku.net.client.GameClientInterface;
import be.mapariensis.kanjiryoku.net.client.ServerUplink;

public interface GUIBridge {
	public ServerUplink getUplink();
	public GameClientInterface getClient();
	public ChatInterface getChat();
}
