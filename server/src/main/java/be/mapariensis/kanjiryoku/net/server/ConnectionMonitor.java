package be.mapariensis.kanjiryoku.net.server;

import static be.mapariensis.kanjiryoku.net.Constants.GREETING;
import static be.mapariensis.kanjiryoku.net.Constants.protocolMajorVersion;
import static be.mapariensis.kanjiryoku.net.Constants.protocolMinorVersion;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.config.ServerConfig;
import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.commands.ClientCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException.Type;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.IMessageHandler;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.PlainMessageHandler;
import be.mapariensis.kanjiryoku.net.model.SSLMessageHandler;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.model.UserStore;
import be.mapariensis.kanjiryoku.net.secure.SSLContextUtil;
import be.mapariensis.kanjiryoku.net.server.handlers.AdminTaskExecutor;
import be.mapariensis.kanjiryoku.net.server.handlers.CommandReceiverFactory;
import be.mapariensis.kanjiryoku.util.IProperties;

public class ConnectionMonitor extends Thread implements UserManager, Closeable {
	private static final Logger log = LoggerFactory
			.getLogger(ConnectionMonitor.class);
	private static final long SELECT_TIMEOUT = 500;
	private final ExecutorService threadPool;
	private volatile boolean keepOn = true;
	private final ServerSocketChannel ssc;
	private final Selector selector;
	private final UserStore store = new UserStore();
	private final SessionManagerImpl sessman;
	private final ServerConfig config;
	// SSL-related stuff
	private final SSLContext sslContext;
	private final ExecutorService delegatedTaskPool;
	private final int plaintextBufsize;
	private final boolean enforceSSL;
	private final CommandReceiverFactory crf;

	public ConnectionMonitor(ServerConfig config) throws IOException,
			BadConfigurationException {
		this.config = config;
		int port = config.getRequired(ConfigFields.PORT, Integer.class);
		// get a socket
		ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(port));
		selector = Selector.open();
		int workerThreads = config.getTyped(ConfigFields.WORKER_THREADS,
				Integer.class, ConfigFields.WORKER_THREADS_DEFAULT);
		plaintextBufsize = config.getTyped(ConfigFields.PLAINTEXT_BUFFER_SIZE,
				Integer.class, ConfigFields.PLAINTEXT_BUFFER_SIZE_DEFAULT);
		enforceSSL = config.getTyped(ConfigFields.FORCE_SSL, Boolean.class,
				ConfigFields.FORCE_SSL_DEFAULT);
		threadPool = Executors.newFixedThreadPool(workerThreads);
		sessman = new SessionManagerImpl(config, this);
		int usernameCharLimit = config.getTyped(ConfigFields.USERNAME_LIMIT,
				Integer.class, ConfigFields.USERNAME_LIMIT_DEFAULT);
		crf = new CommandReceiverFactory(usernameCharLimit, this, selector,
				sessman);
		setName("ConnectionMonitor:" + port);
		IProperties sslConfig = config
				.getTyped("sslContext", IProperties.class);
		if (sslConfig != null) {
			sslContext = SSLContextUtil.setUp(sslConfig);
			delegatedTaskPool = Executors.newFixedThreadPool(workerThreads);
		} else if (!enforceSSL) {
			log.info("No SSL configuration found. Starting server in plaintext-only mode.");
			sslContext = null;
			delegatedTaskPool = null;
		} else {
			throw new BadConfigurationException(
					"Server was instructed to enforce SSL, but no SSL configuration could be found.");
		}
	}

	private void setMode(SelectionKey key, String mode) throws IOException {
		SocketChannel ch = (SocketChannel) key.channel();
		// caller checks this
		PlainMessageHandler h = (PlainMessageHandler) key.attachment();
		if (enforceSSL
				|| (Constants.MODE_TLS.equals(mode) && sslContext != null)) {
			// Send SETMODE before switching, otherwise the client won't
			// understand
			queueMessage(ch, new NetworkMessage(ClientCommandList.SETMODE,
					Constants.MODE_TLS));
			h.setLimbo();
		} else {
			// we don't have to do anything for other modes
			queueMessage(ch, new NetworkMessage(ClientCommandList.SETMODE,
					Constants.MODE_PLAIN));
			h.setPermanent();
		}
	}

	private SSLMessageHandler activateSsl(SelectionKey key) {
		SocketChannel ch = (SocketChannel) key.channel();
		log.trace("Activating SSL for {}", ch.socket());
		String addr = ch.socket().getInetAddress().toString();
		int port = ch.socket().getPort();
		SSLEngine engine = sslContext.createSSLEngine(addr, port);
		engine.setUseClientMode(false);
		engine.setNeedClientAuth(false);
		engine.setWantClientAuth(false);
		SSLMessageHandler h = new SSLMessageHandler(key, engine,
				delegatedTaskPool, plaintextBufsize);
		key.attach(h);
		return h;
	}

	@Override
	public void run() {
		// main thread that dispatches workers als necessary
		try {
			ssc.configureBlocking(false);
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			log.info("Started listening for new connections.");
		} catch (IOException e1) {
			log.error("Failed to register socket.", e1);
			try {
				close();
			} catch (IOException e) {
			}
			return;
		}
		while (keepOn) {
			int readyCount;
			try {
				readyCount = selector.select(SELECT_TIMEOUT);
			} catch (IOException e) {
				log.error("I/O error during selection", e);
				break;
			}
			if (readyCount == 0)
				continue;
			// use iterator to remove keys after handling them
			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
			while (iter.hasNext()) {
				SelectionKey key = iter.next();
				iter.remove();
				try {
					if (!key.isValid()) {
						key.cancel();
						continue;
					}
					if (!key.channel().isOpen()) {
						log.info("Channel {} closed.", key.channel());
						deregisterAndClose(key);
						continue;
					}
					if (key.isAcceptable()) {
						acceptClient();
					}
					if (key.isReadable()) {
						readClient(key);
					}
					if (key.isWritable()) {
						IMessageHandler h = (IMessageHandler) key.attachment();
						h.flushMessageQueue();
					}
				} catch (EOFException ex) {
					log.debug("Peer shut down.");
					deregisterAndClose(key);
				} catch (IOException | RuntimeException ex) {
					log.warn("Error while processing {}. Disconnecting.",
							key.channel(), ex);
					deregisterAndClose(key);
				}
			}

		}
		log.info("Connection listener shut down.");
		for (User u : store) {
			deregister(u);
		}
		threadPool.shutdownNow();
		delegatedTaskPool.shutdownNow();
	}

	private void acceptClient() throws IOException {
		SocketChannel ch = ssc.accept();
		log.info("Accepted connection from peer {}", ch);
		ch.configureBlocking(false);
		// register a message handler
		SelectionKey newKey = ch.register(selector, SelectionKey.OP_READ);
		IMessageHandler h = new PlainMessageHandler(newKey, plaintextBufsize);
		newKey.attach(h);
		log.info("Registered message handler.");

		queueMessage(ch, new NetworkMessage(ClientCommandList.HELLO, GREETING));
		queueMessage(ch, new NetworkMessage(ClientCommandList.VERSION,
				protocolMajorVersion, protocolMinorVersion));
	}

	private void readClient(SelectionKey key) throws IOException, EOFException {
		SocketChannel ch = (SocketChannel) key.channel();
		IMessageHandler h = (IMessageHandler) key.attachment();
		if (h instanceof PlainMessageHandler
				&& ((PlainMessageHandler) h).getStatus() == PlainMessageHandler.Status.LIMBO) {
			// we need to switch message handlers first
			h = activateSsl(key);
		}
		List<NetworkMessage> msgs;

		msgs = h.readRaw();
		for (NetworkMessage msg : msgs) {
			// schedule command interpretation
			if (!msg.isEmpty()) {
				String command = msg.get(0);
				if (msg.argCount() == 2
						&& ServerCommand.HELLO.name().equals(command)) {
					// handle mode switching in the same thread
					setMode(key, msg.get(1));
				} else {
					threadPool.execute(crf.getReceiver(ch, msg));
				}
			}
		}

	}

	void stopListening() {
		log.info("Received listener shutdown request.");
		keepOn = false;
	}

	private void queueMessage(SocketChannel ch, NetworkMessage message)
			throws IOException {
		try {
			IMessageHandler h = (IMessageHandler) ch.keyFor(selector)
					.attachment();
			h.send(message);
		} catch (CancelledKeyException | NullPointerException ex) {
			// if we get a CancelledKeyException here, this means the user has
			// already been deregistered, and
			// ensureHandler failed to set up a handler for the connection in
			// question. In other words, this thread attempted to
			// write a message after a remote disconnect.
			log.warn("Failed to write message, peer already disconnected.");
		}
	}

	private void queueProcessingError(SocketChannel ch, ServerException ex)
			throws IOException {
		queueMessage(ch, ex.protocolMessage);
	}

	@Override
	public void close() throws IOException {
		Set<SelectionKey> keys = selector.keys();
		for (SelectionKey key : keys) {
			try {
				key.channel().close();
			} catch (IOException e) {
			} // letting this one slip risks a resource leak
			key.cancel();
		}
		selector.close();
	}

	@Override
	public void finalize() {
		try {
			log.warn("Mopping up unclosed connection monitor.");
			close();
		} catch (IOException e) {
		}
	}

	@Override
	public void register(User user) throws UserManagementException {
		store.addUser(user);

		// message behaviour for remaining messages in the old handler is
		// undefined now, but this shouldn't really matter
		messageUser(user, new NetworkMessage(ClientCommandList.WELCOME,
				user.handle));
		log.info("Registered user {}", user);
		lobbyBroadcast(
				user,
				new NetworkMessage(ClientCommandList.SAY, String.format(
						"User %s entered the room.", user.handle)));
	}

	@Override
	public void deregister(User user) {
		// TODO allow for disconnect handlers?
		if (user == null)
			throw new IllegalArgumentException("null is not a user");
		String username = user.handle;
		user.purgeResponseHandlers();
		boolean nosession = user.getSession() == null;
		if (!nosession) {
			try {
				sessman.removeUser(user);
			} catch (SessionException e) {
				new UserManagementException(e);
			}
		}
		store.removeUser(user);
		log.info("Deregistered user {}", user);
		try {
			((IMessageHandler) user.channel.keyFor(selector).attachment())
					.close();
		} catch (IOException e) {
			log.warn("Error while closing messageHandler", e);
		} finally {
			closeQuietly(user.channel.keyFor(selector));
		}
		if (nosession)
			lobbyBroadcast(user, new NetworkMessage(ClientCommandList.SAY,
					String.format("User %s has disconnected.", username)));
	}

	private void closeQuietly(SelectionKey key) {
		key.cancel();
		try {
			key.channel().close();
		} catch (IOException e) {
		}
	}

	private void deregisterAndClose(SelectionKey key) {
		User u;
		if ((u = store.getUser((SocketChannel) key.channel())) != null) {
			deregister(u);
		} else {
			closeQuietly(key);
		}
	}

	@Override
	public User getUser(String handle) throws UserManagementException {
		return store.requireUser(handle);
	}

	@Override
	public void humanMessage(User user, String message) {
		messageUser(user, new NetworkMessage(ClientCommandList.SAY, message));

	}

	@Override
	public void messageUser(User user, NetworkMessage message,
			ClientResponseHandler handler) {
		messageUser(user, message);
		if (handler != null)
			user.enqueueActiveResponseHandler(handler);
	}

	@Override
	public void messageUser(User user, NetworkMessage message) {
		try {
			queueMessage(user.channel, message);
		} catch (IOException e) {
			log.warn("Failed to message user {}, disconnecting.", user);
			// this might happen during deregistration, so just cancel the key
			user.channel.keyFor(selector).cancel();
		}
	}

	private static enum AdminCommand {
		BROADCAST {
			@Override
			public Runnable getTask(final User issuer,
					final ConnectionMonitor mon, final NetworkMessage command)
					throws ArgumentCountException {
				if (command.argCount() < 2)
					throw new ArgumentCountException(Type.TOO_FEW, BROADCAST);
				return new Runnable() {
					@Override
					public void run() {
						NetworkMessage message = new NetworkMessage(
								ClientCommandList.SAY, String.format(
										"[GLOBAL from %s]\n%s", issuer.handle,
										command.get(1)));
						for (User u : mon.store) {
							mon.messageUser(u, message);
						}
					}
				};
			}
		},
		KICK {

			@Override
			public Runnable getTask(User issuer, final ConnectionMonitor mon,
					NetworkMessage command) throws ProtocolSyntaxException {
				if (command.argCount() < 2)
					throw new ArgumentCountException(Type.TOO_FEW, KICK);
				String username = command.get(1);
				final User toBeKicked;
				try {
					toBeKicked = mon.getUser(username);
				} catch (UserManagementException e) {
					log.warn("User {} not found, aborting.", username, e);
					return null;
				}
				return new Runnable() {

					@Override
					public void run() {
						mon.deregister(toBeKicked);
					}

				};
			}

		},
		NUKESESSION {
			@Override
			public Runnable getTask(User issuer, final ConnectionMonitor mon,
					NetworkMessage command) throws ProtocolSyntaxException {
				if (command.argCount() < 2)
					throw new ArgumentCountException(Type.TOO_FEW, NUKESESSION);
				final Session target;
				try {
					target = mon.sessman.getSession(Integer.parseInt(command
							.get(1)));
				} catch (Exception e) {
					log.warn("Exception in NUKESESSION prep", e);
					return null;
				}

				return target == null ? null : new Runnable() {

					@Override
					public void run() {
						mon.sessman.destroySession(target);
					}
				};
			}
		},
		SHUTDOWN {

			@Override
			public Runnable getTask(User issuer, final ConnectionMonitor mon,
					NetworkMessage command) throws ProtocolSyntaxException {
				return new Runnable() {

					@Override
					public void run() {
						mon.keepOn = false;
					}
				};
			}

		};
		public abstract Runnable getTask(User issuer, ConnectionMonitor mon,
				NetworkMessage command) throws ProtocolSyntaxException;
	}

	@Override
	public void adminCommand(User issuer, int id, NetworkMessage commandMessage)
			throws UserManagementException, ProtocolSyntaxException {
		boolean adminEnabled = config.getSafely(ConfigFields.ENABLE_ADMIN,
				Boolean.class, ConfigFields.ENABLE_ADMIN_DEFAULT);
		@SuppressWarnings("unchecked")
		List<String> allowedIps = config.getSafely(
				ConfigFields.ADMIN_WHITELIST, List.class,
				ConfigFields.ADMIN_WHITELIST_DEFAULT);
		if (!adminEnabled) {
			try {
				queueProcessingError(issuer.channel, new ServerException(
						"Admin commands are disabled.",
						ServerException.ERROR_GENERIC));
			} catch (IOException e) {
				log.error("Failed to issue server error.");
			}
			return;
		}
		if (commandMessage.argCount() == 0)
			throw new ArgumentCountException(Type.TOO_FEW, ServerCommand.ADMIN); // never
																					// hurts
																					// to
																					// be
																					// extra
																					// sure
		InetAddress addr;
		try {
			addr = ((InetSocketAddress) issuer.channel.getRemoteAddress())
					.getAddress();
		} catch (IOException e) {
			log.error("Failed to get command issuer address. Aborting operation.");
			return;
		}
		if (!addr.isLoopbackAddress() && !checkAddress(addr, allowedIps)) {
			// this is a weak security precaution that isn't even that hard to
			// circumvent
			// still, admin commands should not expose anything vital
			log.warn(
					"Warning: non-loopback address {} (bound to user {}) issued admin command. Silently ignoring.",
					addr, issuer.handle);
			return;
		}
		ClientResponseHandler rh;
		try {
			Runnable task = AdminCommand.valueOf(
					commandMessage.get(0).toUpperCase()).getTask(issuer, this,
					commandMessage);
			if (task == null)
				return;
			rh = new AdminTaskExecutor(issuer, id, task);
		} catch (IllegalArgumentException ex) {
			throw new ProtocolSyntaxException("Unknown command "
					+ commandMessage.get(0));
		}
		messageUser(issuer, new NetworkMessage(ClientCommandList.CONFIRMADMIN,
				id, rh.id), rh);
	}

	private static boolean checkAddress(InetAddress addr,
			List<String> allowedIps) {
		for (String s : allowedIps) {
			try {
				if (InetAddress.getByName(s).equals(addr))
					return true;
			} catch (UnknownHostException e) {
				log.warn("Unknown host {}", s);
				continue;
			}
		}
		return false;
	}

	@Override
	public void lobbyBroadcast(User user, NetworkMessage msg) {
		for (User u : store) {
			if (u.getSession() == null && !u.equals(user))
				messageUser(u, msg); // only message fellow users that are not
										// in a session
		}
	}

	@Override
	public UserStore getStore() {
		return store;
	}
}