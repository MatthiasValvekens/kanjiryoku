package be.mapariensis.kanjiryoku.gui;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import be.mapariensis.kanjiryoku.gui.dialogs.CreateSessionDialog;
import be.mapariensis.kanjiryoku.gui.dialogs.SingleInputDialog;
import be.mapariensis.kanjiryoku.net.client.ServerUplink;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class MainWindow extends JFrame implements GUIBridge {
	public static final String CSS_FILE = "chatcss.css";
	private final ServerUplink serv;
	private final ChatInterface chat;
	private final GamePanel gci;
	private final String serverInfoString;
	private final JMenuBar menuBar;

	public MainWindow(InetAddress addr, int port, String username)
			throws IOException {
		this.serverInfoString = String.format("(%s:%s)", addr, port);
		setTitle(String.format("Kanjiryoku - %s", this.serverInfoString));
		setLayout(new FlowLayout());
		String css;
		try (InputStream in = MainWindow.class.getClassLoader()
				.getResourceAsStream(CSS_FILE)) {
			BufferedReader r = new BufferedReader(new InputStreamReader(in,
					Charset.forName("UTF-8")));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null)
				sb.append(line).append('\n');
			css = sb.toString();
		} catch (RuntimeException ex) {
			throw new IOException("Runtime exception while reading css file.",
					ex);
		}
		final HTMLChatPanel chatComponent = new HTMLChatPanel(this, css);
		chat = chatComponent;
		add(new ChatPanel(this, chatComponent));
		serv = new ServerUplink(addr, port, username, this);
		gci = new GamePanel(this);
		add(gci);
		serv.start();
		setResizable(false);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		// clean up on close
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				serv.close();
				System.exit(0);
			}
		});
		// TODO : enable/disable menus based on WHOAMI return values
		// menu bar
		menuBar = new JMenuBar();
		// session menu
		JMenu sessionMenu = new JMenu("Session");
		menuBar.add(sessionMenu);
		sessionMenu.add(new JMenuItem(new AbstractAction("Create") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new CreateSessionDialog(MainWindow.this, serv).setVisible(true);
			}
		}));

		JMenu sessionAdminMenu = new JMenu("Admin");
		sessionMenu.add(sessionAdminMenu);
		sessionAdminMenu.add(new JMenuItem(new AbstractAction("Invite") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new SingleInputDialog(MainWindow.this, "Invite a user",
						"Please provide a username", serv, true) {
					@Override
					protected NetworkMessage constructMessage() {
						return new NetworkMessage(ServerCommandList.INVITE,
								getInput());
					}
				}.setVisible(true);
			}
		}));
		sessionAdminMenu.add(new JMenuItem(new AbstractAction("Kick") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new SingleInputDialog(MainWindow.this, "Kick a user",
						"Please provide a username", serv, true) {
					@Override
					protected NetworkMessage constructMessage() {
						return new NetworkMessage(ServerCommandList.KICK,
								getInput());
					}
				}.setVisible(true);
			}
		}));
		sessionAdminMenu.add(new JMenuItem(new AbstractAction("Kill session") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (JOptionPane.showConfirmDialog(MainWindow.this,
						"Are you sure you want to kill the current session?",
						"Confirm session kill", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					serv.enqueueMessage(new NetworkMessage(
							ServerCommandList.KILLSESSION));
				}
			}

		}));
		sessionMenu.addSeparator();
		sessionMenu.add(new JMenuItem(new AbstractAction("Leave") {
			@Override
			public void actionPerformed(ActionEvent e) {
				serv.enqueueMessage(new NetworkMessage(ServerCommandList.LEAVE));
			}
		}));

		// game menu
		JMenu gameMenu = new JMenu("Game");
		menuBar.add(gameMenu);
		gameMenu.add(new JMenuItem(new AbstractAction("Start game") {
			@Override
			public void actionPerformed(ActionEvent e) {
				serv.enqueueMessage(new NetworkMessage(
						ServerCommandList.STARTGAME));
			}
		}));

		// client menu
		JMenu clientMenu = new JMenu("Client");
		menuBar.add(clientMenu);
		clientMenu.add(new JMenuItem(new AbstractAction("Clear chat") {

			@Override
			public void actionPerformed(ActionEvent e) {
				chatComponent.clear();
			}

		}));

		setJMenuBar(menuBar);
		toggleMenuBar(false);
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

	@Override
	public void setUsername(String username) {
		setTitle(String
				.format("Kanjiryoku - %s %s", username, serverInfoString));
		serv.setUsername(username);
		toggleMenuBar(true);
	}

	private void toggleMenuBar(boolean enable) {
		int count = menuBar.getMenuCount();
		for (int i = 0; i < count; i++) {
			menuBar.getMenu(i).setEnabled(enable);
		}
	}

	@Override
	public Frame getFrame() {
		return this;
	}
}
