package be.mapariensis.kanjiryoku.gui;

import java.awt.FlowLayout;
import java.io.IOException;
import java.net.InetAddress;

import javax.swing.JFrame;

import be.mapariensis.kanjiryoku.net.client.ChatInterface;
import be.mapariensis.kanjiryoku.net.client.GameClientInterface;
import be.mapariensis.kanjiryoku.net.client.ServerUplink;
import be.mapariensis.kanjiryoku.providers.KanjiryokuShindanParser;

public class MainWindow extends JFrame implements GUIBridge {
	private final ServerUplink serv;
	private final ChatPanel chat;
	private final GamePanel gci;
	public MainWindow(InetAddress addr, int port, String username) throws IOException {
		super("Kanji");
		setLayout(new FlowLayout());
		chat = new ChatPanel(this);
		add(chat);
		serv = new ServerUplink(addr, port, username, this);
		gci = new GamePanel(this, new KanjiryokuShindanParser());
		add(gci);
		serv.start();
		setResizable(false);
		pack();
	}

	@Override
	public ServerUplink getUplink() {
		return serv;
	}

	@Override
	public GameClientInterface getClient() {
		return gci;
	}

	@Override
	public ChatInterface getChat() {
		return chat;
	}
}
