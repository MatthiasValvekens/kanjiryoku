package be.mapariensis.kanjiryoku;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.gui.ChatInterface;
import be.mapariensis.kanjiryoku.gui.UIBridge;
import be.mapariensis.kanjiryoku.gui.GameClientInterface;
import be.mapariensis.kanjiryoku.net.client.ServerResponseHandler;
import be.mapariensis.kanjiryoku.net.client.ServerUplink;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class AdminConsole implements UIBridge, ChatInterface {
	private static final Logger log = LoggerFactory
			.getLogger(AdminConsole.class);
	private final ServerUplink uplink;

	public AdminConsole(InetAddress addr, int port, String username)
			throws IOException {
		this.uplink = new ServerUplink(addr, port, username, this);
	}

	@Override
	public ServerUplink getUplink() {
		return uplink;
	}

	@Override
	public GameClientInterface getClient() {
		return null;
	}

	@Override
	public ChatInterface getChat() {
		return this;
	}

	@Override
	public void setUsername(String username) {
		displaySystemMessage(String.format("Username set to %s", username));
		uplink.enqueueMessage(new NetworkMessage(ServerCommandList.ADMIN,
				System.currentTimeMillis() % 10000, "BROADCAST", "elloello")); // test
	}

	@Override
	public void displayServerMessage(long timestamp, String message) {
		printMessage(Constants.SERVER_HANDLE, message);
	}

	@Override
	public void displayGameMessage(long timestamp, String message) {
		displaySystemMessage("We shouldn't be receiving game messages. Logged");
		log.warn("Game message received. This shouldn't happen.", message);
	}

	@Override
	public void displayUserMessage(long timestamp, String from, String message,
			boolean broadcast) {
		printMessage(String.format("[%s]", from), message);
	}

	@Override
	public void displayErrorMessage(int errorId, String message) {
		printMessage(String.format("Error E%03d", errorId), message);

	}

	@Override
	public void displayErrorMessage(ClientServerException ex) {
		displayErrorMessage(ex.errorCode, ex.getMessage());
	}

	@Override
	public void displaySystemMessage(String message) {
		printMessage(Constants.SYSTEM_HANDLE, message);
	}

	private void printMessage(String origin, String message) {
		System.out.println(String.format("%s\t%s", origin, message));
	}

	@Override
	public void yesNoPrompt(String question, NetworkMessage ifYes,
			NetworkMessage ifNo) {
		// TODO make this an actual prompt, unless in auto task mode
		uplink.enqueueMessage(ifYes);
	}

	@Override
	public ServerResponseHandler getDefaultResponseHandler() {
		return new ServerResponseHandler() {

			@Override
			public void handle(NetworkMessage msg) throws ClientException {
				printMessage("Response", msg.toString());
			}
		};
	}

	public void start() {
		uplink.start();
	}

	public static void main(String[] args) throws UnknownHostException,
			IOException {
		// Placeholder
		AdminConsole console = new AdminConsole(
				InetAddress.getByName("127.0.0.1"), 9630, "admin"
						+ (System.currentTimeMillis() % 10000));
		console.start();
	}
}
