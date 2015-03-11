package be.mapariensis.kanjiryoku.net.secure.auth;

import java.security.SecureRandom;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.commands.ClientCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.AuthenticationFailedException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

// The authentication state machine.
public class ServerAuthEngine {

	private static final Logger log = LoggerFactory
			.getLogger(ServerAuthEngine.class);

	public static enum AuthStatus {
		/**
		 * The initial state, nothing has happened yet.
		 */
		INIT,
		/**
		 * The server is waiting for credentials.
		 */
		WAIT_CRED,
		/**
		 * The server is processing the credentials.
		 */
		PROCESS_CRED,
		/**
		 * Authentication succeeded.
		 */
		SUCCESS,
		/**
		 * Authentication failed.
		 */
		FAILURE
	}

	private final SecureRandom rng = new SecureRandom();
	private final AuthHandler.Factory factory;
	private AuthHandler backend;
	private String username;
	private volatile AuthStatus status;
	private boolean stringAlong = false;

	public ServerAuthEngine(AuthHandler.Factory factory) {
		this.status = AuthStatus.INIT;
		this.factory = factory;
	}

	public AuthStatus getStatus() {
		return status;
	}

	public synchronized NetworkMessage submit(NetworkMessage msg)
			throws ProtocolSyntaxException, AuthenticationFailedException {
		try {
			switch (status) {
			case PROCESS_CRED:
			case SUCCESS:
			case FAILURE:
				throw new ProtocolSyntaxException();
			case INIT:
				username = msg.get(1);
				status = AuthStatus.WAIT_CRED;
				try {
					backend = factory.init(username);
				} catch (UserManagementException e) {
					// The user doesn't exist, but let's play along anyway.
					stringAlong = true;
					// doesn't really matter, we only need a vaguely convincing
					// hash
					return new NetworkMessage(BCrypt.gensalt(2, rng));
				}
				String salt = backend.getSalt();
				return new NetworkMessage(ClientCommandList.AUTH, salt);
			case WAIT_CRED:
				String hash = msg.get(1);
				String clientSalt = msg.get(2);
				status = AuthStatus.PROCESS_CRED;
				if (!stringAlong && backend.authenticate(hash, clientSalt)) {
					status = AuthStatus.SUCCESS;
					return new NetworkMessage(ClientCommandList.AUTH,
							Constants.ACCEPTS);
				} else {
					status = AuthStatus.FAILURE;
					throw new AuthenticationFailedException(username);
				}
			}
		} catch (RuntimeException ex) {
			log.info("Authentication failed.", ex);
			status = AuthStatus.FAILURE;
			throw new ProtocolSyntaxException();
		}
		return null;
	}
}
