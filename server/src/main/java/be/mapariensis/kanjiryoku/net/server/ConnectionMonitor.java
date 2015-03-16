package be.mapariensis.kanjiryoku.net.server;

import static be.mapariensis.kanjiryoku.net.Constants.GREETING;
import static be.mapariensis.kanjiryoku.net.Constants.protocolMajorVersion;
import static be.mapariensis.kanjiryoku.net.Constants.protocolMinorVersion;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
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
import be.mapariensis.kanjiryoku.net.secure.SecurityUtils;
import be.mapariensis.kanjiryoku.net.secure.auth.AuthBackendProvider;
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
	private final int plaintextBufsize;
	private final boolean enforceSSL, requireAuth;
	private final AuthBackendProvider authBackendProvider;
	private final CommandReceiverFactory crf;

	public ConnectionMonitor(ServerConfig config) throws IOException,
			BadConfigurationException {
		this.config = config;
		int port = config.getRequired(ConfigFields.PORT, Integer.class);
		// get a socket
		ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(port));
		selector = Selector.open();

		// initialise threading and network settings
		int workerThreads = config.getTyped(ConfigFields.WORKER_THREADS,
				Integer.class, ConfigFields.WORKER_THREADS_DEFAULT);
		plaintextBufsize = config.getTyped(ConfigFields.PLAINTEXT_BUFFER_SIZE,
				Integer.class, ConfigFields.PLAINTEXT_BUFFER_SIZE_DEFAULT);
		enforceSSL = config.getTyped(ConfigFields.FORCE_SSL, Boolean.class,
				ConfigFields.FORCE_SSL_DEFAULT);
		threadPool = Executors.newFixedThreadPool(workerThreads);
		sessman = new SessionManagerImpl(config, this);

		setName("ConnectionMonitor:" + port);

		// set up ssl context
		IProperties sslConfig = config
				.getTyped("sslContext", IProperties.class);
		if (sslConfig != null) {
			sslContext = SecurityUtils.setUp(sslConfig);
		} else if (!enforceSSL) {
			log.info("No SSL configuration found. Starting server in plaintext-only mode.");
			sslContext = null;
		} else {
			throw new BadConfigurationException(
					"Server was instructed to enforce SSL, but no SSL configuration could be found.");
		}

		// set up auth mechanism
		requireAuth = config.getTyped(ConfigFields.REQUIRE_AUTH, Boolean.class,
				ConfigFields.REQUIRE_AUTH_DEFAULT);
		if (requireAuth && sslConfig == null) {
			log.warn("Warning: authentication is enabled, but SSL is not configured properly.");
		}
		if (requireAuth) {
			IProperties authBackend = config.getRequired(
					ConfigFields.AUTH_BACKEND, IProperties.class);
			String providerFactory = authBackend.getRequired(
					ConfigFields.AUTH_BACKEND_PROVIDER_CLASS, String.class);
			// Load factory implementation from config
			AuthBackendProvider.Factory factory;
			try {
				factory = (AuthBackendProvider.Factory) getClass()
						.getClassLoader().loadClass(providerFactory)
						.newInstance();
			} catch (InstantiationException | IllegalAccessException
					| ClassNotFoundException | ClassCastException e) {
				log.error("Failed to instantiate authentication backend.", e);
				throw new BadConfigurationException(e);
			}
			// This contains the configuration for the authentication backend
			// e.g. database credentials, etc.
			IProperties backendConfig = authBackend.getRequired(
					ConfigFields.AUTH_BACKEND_CONFIG, IProperties.class);
			// Initialise auth handler provider.
			authBackendProvider = factory.setUp(backendConfig);
		} else {
			authBackendProvider = null;
		}

		// Set up command handler
		crf = new CommandReceiverFactory(config, this, selector, sessman);
	}

	private void setMode(PlainMessageHandler h, SocketChannel ch, String mode)
			throws IOException {
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
		engine.setWantClientAuth(true);
		SSLMessageHandler h = new SSLMessageHandler(key, engine, threadPool,
				plaintextBufsize);
		((ConnectionContext) key.attachment()).setMessageHandler(h);
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
						IMessageHandler h = ((ConnectionContext) key
								.attachment()).getMessageHandler();
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
	}

	private void acceptClient() throws IOException {
		SocketChannel ch = ssc.accept();
		log.info("Accepted connection from peer {}", ch);
		ch.configureBlocking(false);
		// register a message handler
		SelectionKey newKey = ch.register(selector, SelectionKey.OP_READ);
		IMessageHandler h = new PlainMessageHandler(newKey, plaintextBufsize);
		ConnectionContext context = new ConnectionContext();
		context.setMessageHandler(h);
		newKey.attach(context);
		log.info("Registered message handler.");

		queueMessage(ch, new NetworkMessage(ClientCommandList.HELLO, GREETING));
		queueMessage(ch, new NetworkMessage(ClientCommandList.VERSION,
				protocolMajorVersion, protocolMinorVersion));
	}

	private void readClient(SelectionKey key) throws IOException, EOFException {
		SocketChannel ch = (SocketChannel) key.channel();
		IMessageHandler h = ((ConnectionContext) key.attachment())
				.getMessageHandler();
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
					if (h instanceof PlainMessageHandler) {
						setMode((PlainMessageHandler) h, ch, msg.get(1));
					} else {
						log.info("Can't set mode now, moving on...");
					}
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
			IMessageHandler h = ((ConnectionContext) ch.keyFor(selector)
					.attachment()).getMessageHandler();
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
			((ConnectionContext) user.channel.keyFor(selector).attachment())
					.getMessageHandler().close();
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

	@Override
	public void adminCommand(User issuer, NetworkMessage commandMessage)
			throws UserManagementException, ProtocolSyntaxException {
		boolean adminEnabled = config.getSafely(ConfigFields.ENABLE_ADMIN,
				Boolean.class, ConfigFields.ENABLE_ADMIN_DEFAULT);
		if (!issuer.data.isAdmin()) {
			log.debug("Non-admin {} attempted to use admin command.",
					issuer.handle);
			throw new UserManagementException(String.format(
					"User \"%s\" is not an administrator", issuer.handle));
		}

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
			// never hurts to be extra sure
			throw new ArgumentCountException(Type.TOO_FEW, ServerCommand.ADMIN);
		try {
			AdminCommand.valueOf(commandMessage.get(0).toUpperCase()).execute(
					issuer, this, commandMessage);
		} catch (IllegalArgumentException ex) {
			throw new ProtocolSyntaxException("Unknown command "
					+ commandMessage.get(0));
		}
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

	@Override
	public void delegate(Runnable run) {
		threadPool.execute(run);
	}

	public SessionManager getSessionManager() {
		return sessman;
	}

	void shutdown() {
		keepOn = false;
	}

	@Override
	public AuthBackendProvider getAuthBackend() {
		return authBackendProvider;
	}
}
