package be.mapariensis.kanjiryoku.net.server;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.commands.ClientCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException.Type;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.handlers.SignupResponseHandler;

public enum AdminCommand {

	BROADCAST {
		@Override
		public void execute(final User issuer, final ConnectionMonitor mon,
				final NetworkMessage command) throws ArgumentCountException {
			if (command.argCount() < 2)
				throw new ArgumentCountException(Type.TOO_FEW, BROADCAST);
			NetworkMessage message = new NetworkMessage(ClientCommandList.SAY,
					String.format("[GLOBAL from %s]\n%s", issuer.handle,
							command.get(1)));
			for (User u : mon.getStore()) {
				mon.messageUser(u, message);
			}
		}
	},
	KICK {

		@Override
		public void execute(User issuer, final ConnectionMonitor mon,
				NetworkMessage command) throws ProtocolSyntaxException {
			if (command.argCount() < 2)
				throw new ArgumentCountException(Type.TOO_FEW, KICK);
			String username = command.get(1);
			final User toBeKicked;
			try {
				toBeKicked = mon.getUser(username);
			} catch (UserManagementException e) {
				log.warn("User {} not found, aborting.", username, e);
				return;
			}
			mon.deregister(toBeKicked);
		}

	},
	NUKESESSION {
		@Override
		public void execute(User issuer, final ConnectionMonitor mon,
				NetworkMessage command) throws ProtocolSyntaxException {
			if (command.argCount() < 2)
				throw new ArgumentCountException(Type.TOO_FEW, NUKESESSION);
			final Session target;
			try {
				target = mon.getSessionManager().getSession(
						Integer.parseInt(command.get(1)));
			} catch (Exception e) {
				log.warn("Exception in NUKESESSION prep", e);
				return;
			}

			if (target != null)
				mon.getSessionManager().destroySession(target);
		}
	},
	SHUTDOWN {

		@Override
		public void execute(User issuer, final ConnectionMonitor mon,
				NetworkMessage command) throws ProtocolSyntaxException {
			mon.shutdown();
		}

	},
	ADDUSER {

		@Override
		public void execute(User issuer, ConnectionMonitor mon,
				NetworkMessage command) throws ProtocolSyntaxException {
			if (command.argCount() < 3)
				throw new ArgumentCountException(Type.TOO_FEW, ADDUSER);
			if (!issuer.isConnectionSecure())
				throw new ProtocolSyntaxException(
						"Cannot execute this command over plaintext connection");
			if (mon.getAuthBackend() == null) {
				mon.humanMessage(issuer, "No auth backend available.");
				return;
			}
			int responseCode;
			try {
				responseCode = Integer.parseInt(command.get(1));
			} catch (RuntimeException ex) {
				throw new ProtocolSyntaxException(ex);
			}
			String username = command.get(2);
			String salt = BCrypt.gensalt();
			ClientResponseHandler rh = new SignupResponseHandler(username,
					salt, mon);
			NetworkMessage reply = new NetworkMessage(
					ClientCommandList.RESPOND, responseCode, rh.id, salt);
			mon.messageUser(issuer, reply, rh);
		}

	},
	DELUSER {

		@Override
		public void execute(User issuer, ConnectionMonitor mon,
				NetworkMessage command) throws ProtocolSyntaxException {
			if (command.argCount() != 2)
				throw new ArgumentCountException(Type.UNEQUAL, DELUSER);
			String username = command.get(1);
			if (mon.getAuthBackend() == null) {
				mon.humanMessage(issuer, "No auth backend available.");
				return;
			}
			if (mon.getStore().getUser(username) != null) {
				mon.humanMessage(issuer, "Cannot remove user while logged in.");
				return;
			}
			try {
				mon.getAuthBackend().deleteUser(username);
				mon.humanMessage(issuer,
						String.format("Deleted user %s.", username));
			} catch (ServerBackendException e) {
				log.error("Exception while attempting to delete user {}",
						username, e);
				mon.humanMessage(issuer,
						String.format("Failed to delete user {}.", username));
			}
		}

	};

	private static final Logger log = LoggerFactory
			.getLogger(AdminCommand.class);

	public abstract void execute(User issuer, ConnectionMonitor mon,
			NetworkMessage command) throws ProtocolSyntaxException;
}
