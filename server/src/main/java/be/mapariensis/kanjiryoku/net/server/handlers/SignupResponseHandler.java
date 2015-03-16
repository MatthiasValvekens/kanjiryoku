package be.mapariensis.kanjiryoku.net.server.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException.Type;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.ClientResponseHandler;
import be.mapariensis.kanjiryoku.net.server.ServerCommand;
import be.mapariensis.kanjiryoku.net.server.UserManager;

public class SignupResponseHandler extends ClientResponseHandler {
	private static final Logger log = LoggerFactory
			.getLogger(SignupResponseHandler.class);

	private final String username, salt;
	private final UserManager userman;

	public SignupResponseHandler(String username, String salt,
			UserManager userman) {
		this.username = username;
		this.salt = salt;
		this.userman = userman;
	}

	@Override
	public void handle(User user, NetworkMessage msg) throws ServerException {
		if (msg.argCount() != 3)
			throw new ArgumentCountException(Type.UNEQUAL,
					ServerCommand.RESPOND);
		String hash = msg.get(2);
		userman.getAuthBackend().createUser(username, hash, salt);
		log.info("Registered user {}.", username);
		userman.humanMessage(user,
				String.format("Registered user %s.", username));
	}

}
