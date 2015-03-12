package be.mapariensis.kanjiryoku.net.server.handlers;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.IMessageHandler;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.SSLMessageHandler;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.ServerCommand;
import be.mapariensis.kanjiryoku.net.server.SessionManager;
import be.mapariensis.kanjiryoku.net.server.UserManager;

public class CommandReceiverFactory {
	private static final Logger log = LoggerFactory
			.getLogger(CommandReceiverFactory.class);
	private final int usernameCharLimit;
	private final UserManager userman;
	private final Selector selector;
	private final SessionManager sessman;

	public CommandReceiverFactory(int usernameCharLimit, UserManager userman,
			Selector selector, SessionManager sessman) {
		this.usernameCharLimit = usernameCharLimit;
		this.userman = userman;
		this.selector = selector;
		this.sessman = sessman;
	}

	public Runnable getReceiver(SocketChannel ch, NetworkMessage msg) {
		return new CommandReceiver(ch, msg);
	}

	private class CommandReceiver implements Runnable {
		final SocketChannel ch;
		final NetworkMessage msg;

		CommandReceiver(SocketChannel ch, NetworkMessage msg) {
			this.ch = ch;
			this.msg = msg;
		}

		@Override
		public void run() {
			try {
				ServerCommand command;
				String commandString = msg.get(0).toUpperCase();
				try {
					command = ServerCommand.valueOf(commandString);
				} catch (IllegalArgumentException ex) {
					throw new ProtocolSyntaxException(String.format(
							"Unknown command %s", commandString));
				}
				// check for REGISTER command (which gets special treatment)
				if (command == ServerCommand.REGISTER) {
					String handle = msg.get(1);
					handle = handle.substring(0,
							Math.min(usernameCharLimit, handle.length())); // truncate
																			// handle
					SelectionKey key = ch.keyFor(selector);
					if (key == null) {
						log.error("Key cancelled before registration could complete! Aborting.");
						SSLMessageHandler h = (SSLMessageHandler) ch.keyFor(
								selector).attachment();
						if (h != null)
							h.close();
						return;
					}
					IMessageHandler h = (IMessageHandler) ch.keyFor(selector)
							.attachment();
					userman.register(new User(handle, ch, h));
				} else {
					User u = userman.getStore().getUser(ch);
					if (u == null) {
						if (command == ServerCommand.BYE) {
							log.info(
									"Gracefully closing {} disconnected with BYE",
									ch);
							SSLMessageHandler h = (SSLMessageHandler) ch
									.keyFor(selector).attachment();
							h.close();
						} else if (command != ServerCommand.HELLO)
							throw new UserManagementException(
									"You must register before using any command other than HELLO, REGISTER or BYE");
					} else
						command.execute(msg, u, userman, sessman);
				}
			} catch (ServerException ex) {
				log.debug("Processing error", ex);
				queueProcessingError(ch, ex);
			} catch (IndexOutOfBoundsException ex) {
				log.debug("Badly formed command: {}", msg, ex);
				queueProcessingError(ch, new ProtocolSyntaxException(
						"Badly formed command", ex));
			} catch (Exception e) {
				log.error("Failed to process command.", e);
				queueProcessingError(ch, new ServerBackendException(e));
			}
		}

	}

	private void queueProcessingError(SocketChannel ch, ServerException ex) {

		try {
			IMessageHandler h = (IMessageHandler) ch.keyFor(selector)
					.attachment();
			h.send(ex.protocolMessage);
		} catch (CancelledKeyException | NullPointerException | IOException e) {
			log.warn("Failed to write message, peer already disconnected.");
		}
	}
}