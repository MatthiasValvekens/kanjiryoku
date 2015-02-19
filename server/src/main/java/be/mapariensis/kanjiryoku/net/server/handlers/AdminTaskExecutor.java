package be.mapariensis.kanjiryoku.net.server.handlers;

import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException.Type;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.ClientResponseHandler;
import be.mapariensis.kanjiryoku.net.server.ServerCommand;

public class AdminTaskExecutor extends ClientResponseHandler {
	private final Runnable task;
	private final User initiator;
	private final int id;

	public AdminTaskExecutor(User initiator, int id, Runnable task) {
		this.task = task;
		this.initiator = initiator;
		this.id = id;
	}

	@Override
	public void handle(User user, NetworkMessage msg) throws ServerException {
		if (msg.argCount() < 3)
			throw new ArgumentCountException(Type.TOO_FEW, ServerCommand.ADMIN);
		int id;
		try {
			id = Integer.parseInt(msg.get(2));
		} catch (RuntimeException ex) {
			throw new ProtocolSyntaxException(ex);
		}
		if (!initiator.equals(user) || this.id != id) {
			throw new UserManagementException(
					"Admin command issuer does not match responder or issue ID is incorrect. Aborting.");
		} else {
			task.run();
		}
	}

}
