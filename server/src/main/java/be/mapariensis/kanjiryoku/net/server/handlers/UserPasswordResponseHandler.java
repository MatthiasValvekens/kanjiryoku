package be.mapariensis.kanjiryoku.net.server.handlers;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.commands.ClientCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException.Type;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.ClientResponseHandler;
import be.mapariensis.kanjiryoku.net.server.ServerCommand;
import be.mapariensis.kanjiryoku.net.server.UserManager;

public class UserPasswordResponseHandler extends ClientResponseHandler {
	private static final Logger log = LoggerFactory
			.getLogger(UserPasswordResponseHandler.class);

	private final String username, salt;
	private final UserManager userman;
	private final boolean newUser;

	public UserPasswordResponseHandler(String username, String salt,
			UserManager userman, boolean newUser) {
		this.username = username;
		this.salt = salt;
		this.userman = userman;
		this.newUser = newUser;
	}

	@Override
	public void handle(User user, NetworkMessage msg) throws ServerException {
		if (msg.argCount() != 3)
			throw new ArgumentCountException(Type.UNEQUAL,
					ServerCommand.RESPOND);
		String hash = msg.get(2);
		if (newUser) {
			userman.getAuthBackend().createUser(username, hash, salt);
		} else {
			userman.getAuthBackend().changePassword(username, hash, salt);
		}
		log.info("Registered user {}.", username);
		userman.humanMessage(user,
				String.format("Registered user %s.", username));
	}

	public static void processPasswordOperation(User issuer, UserManager man,
			String username, int responseCode, boolean signup)
			throws ProtocolSyntaxException {
		if (!issuer.isConnectionSecure())
			throw new ProtocolSyntaxException(
					"Cannot execute this command over plaintext connection");
		if (man.getAuthBackend() == null) {
			man.humanMessage(issuer, "No auth backend available.");
			return;
		}

		String salt = BCrypt.gensalt();
		ClientResponseHandler rh = new UserPasswordResponseHandler(username,
				salt, man, signup);
		NetworkMessage reply = new NetworkMessage(ClientCommandList.RESPOND,
				responseCode, rh.id, salt);
		man.messageUser(issuer, reply, rh);
	}
}
