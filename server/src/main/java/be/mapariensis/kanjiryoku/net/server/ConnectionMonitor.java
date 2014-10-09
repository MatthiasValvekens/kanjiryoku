package be.mapariensis.kanjiryoku.net.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static be.mapariensis.kanjiryoku.net.Constants.*;
import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.config.IProperties;
import be.mapariensis.kanjiryoku.cr.KanjiGuesserFactory;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.commands.ClientCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException.Type;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.MessageHandler;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.model.UserStore;
import be.mapariensis.kanjiryoku.net.server.handlers.AdminTaskExecutor;
import be.mapariensis.kanjiryoku.net.util.MessageFragmentBuffer;
import be.mapariensis.kanjiryoku.net.util.NetworkThreadFactory;

public class ConnectionMonitor extends Thread implements UserManager, Closeable {
	private static final Logger log = LoggerFactory.getLogger(ConnectionMonitor.class);
	private static final long SELECT_TIMEOUT = 500;
	private final ExecutorService threadPool;
	private volatile boolean keepOn = true;
	private final ServerSocketChannel ssc;
	private final Selector selector;
	private final UserStore store = new UserStore();
	private final SessionManagerImpl sessman;
	private final int bufferMax;
	private final int usernameCharLimit;
	private final Runnable rereadConfig;
	private final IProperties config;
	// store message handlers for anonymous connections here until they register/identify
	private final Map<SocketChannel,MessageHandler> strayHandlers = new ConcurrentHashMap<SocketChannel,MessageHandler>();
	public ConnectionMonitor(IProperties config, Runnable rereadConfig) throws IOException, BadConfigurationException {
		this.config = config;
		int port = config.getRequired(ConfigFields.PORT,Integer.class);
		// get a socket
		ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(port));
		selector = Selector.open();
		int workerThreads = config.getTyped(ConfigFields.WORKER_THREADS, Integer.class,ConfigFields.WORKER_THREADS_DEFAULT);
		bufferMax = config.getTyped(ConfigFields.WORKER_BUFFER_SIZE, Integer.class,ConfigFields.WORKER_BUFFER_SIZE_DEFAULT);
		threadPool = Executors.newFixedThreadPool(workerThreads, new NetworkThreadFactory(bufferMax,selector));
		
		// load guesser factory
		IProperties crSettings = config.getRequired(ConfigFields.CR_SETTINGS_HEADER,IProperties.class);
		KanjiGuesserFactory factory;
		String className = crSettings.getRequired(ConfigFields.GUESSER_FACTORY_CLASS, String.class);
		try {
			log.info("Loading guesser factory {}",className);
			factory = (KanjiGuesserFactory) ConnectionMonitor.class.getClassLoader().loadClass(className).newInstance();
		} catch (Exception ex) {
			throw new BadConfigurationException("Failed to instantiate guesser factory.",ex);
		}
		sessman = new SessionManagerImpl(config,this,factory);
		usernameCharLimit = config.getTyped(ConfigFields.USERNAME_LIMIT, Integer.class, ConfigFields.USERNAME_LIMIT_DEFAULT);
		this.rereadConfig = rereadConfig;
		
		
		
		setName("ConnectionMonitor:"+port);
	}
	@Override
	public void run() {
		// main thread that dispatches workers als necessary
		try {
			ssc.configureBlocking(false);
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			log.info("Started listening for new connections.");
		} catch (IOException e1) {
			log.error("Failed to register socket.",e1);
			try {
				close();
			} catch (IOException e) {}
			return;
		}
		ByteBuffer messageBuffer = ByteBuffer.allocateDirect(bufferMax); // allocate one buffer for the monitor thread
		while(keepOn) {
			int readyCount;
			try {
				readyCount = selector.select(SELECT_TIMEOUT);
			} catch (IOException e) {
				log.error("I/O error during selection",e);
				break;
			}
			if(readyCount == 0) continue;
			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();//use iterator to remove keys after handling them
			while(iter.hasNext()) {
				SelectionKey key = iter.next();
				iter.remove();
				SocketChannel ch = null;
				try {
					if(key.isAcceptable()) {
						assert key.channel() == ssc;
						ch = ssc.accept();
						log.info("Accepted connection from peer {}",ch);
						ch.configureBlocking(false);
						SelectionKey channelKey = ch.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
						channelKey.attach(new MessageFragmentBuffer(bufferMax));
						queueMessage(ch, new NetworkMessage(ClientCommandList.SAY,GREETING));
						queueMessage(ch, new NetworkMessage(ClientCommandList.VERSION,protocolMajorVersion,protocolMinorVersion));
					} else if(key.isReadable()) {
						ch = (SocketChannel) key.channel();
						List<NetworkMessage> msgs;
						try {
							Object attachment = key.attachment();
							if(!(attachment instanceof MessageFragmentBuffer)) {
								log.info("No message fragment buffer in key attachment. Aborting");
								key.cancel();
								break;
							}
							msgs = NetworkMessage.readRaw(ch, messageBuffer,(MessageFragmentBuffer) attachment);
						} catch(IOException ex) {  //FIXME : figure out a way to deal with forcefully closed connections, and then downgrade this to EOFException
							log.info("Peer {} shut down.", ch.getRemoteAddress());
							key.cancel();
							User u;
							if((u = store.getUser(ch))!= null) {
								deregister(u);
							}
							continue;
						} 
						for(NetworkMessage msg : msgs) {
							// schedule command interpretation
							if(!msg.isEmpty()) threadPool.execute(new CommandReceiver(ch, msg));
						}
					} else if(key.isWritable()) {
						ch = (SocketChannel) key.channel();
						// deregister write event
						MessageHandler h = ensureHandler(ch);
						threadPool.execute(h);
					}

				} catch(Exception ex) {
					log.warn("Error while processing {}. Ignoring.",ch != null ? ch : "(unknown address)",ex);
				}
			}

		}
		log.info("Connection listener shut down.");
		for(User u : store) {
			try {
				deregister(u);
			} catch (UserManagementException e) {
				log.info("Failed to deregister {}",u);
			}
		}
		threadPool.shutdownNow();
	}

	void stopListening() {
		log.info("Received listener shutdown request.");
		keepOn = false;
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
					throw new ProtocolSyntaxException(String.format("Unknown command %s",commandString));
				}
				// check for REGISTER command (which gets special treatment)
				if(command == ServerCommand.REGISTER) {
					String handle = msg.get(1);
					handle = handle.substring(0,Math.min(usernameCharLimit, handle.length())); // truncate handle
					SelectionKey key = ch.keyFor(selector);
					if(key == null) {
						log.error("Key cancelled before registration could complete! Aborting.");
						strayHandlers.remove(ch);
						return;
					}
					MessageHandler h = new MessageHandler(key);
					register(new User(handle,ch,h));
				} else {
					User u = store.getUser(ch);
					if(u == null) {
						if(command == ServerCommand.BYE) {
							log.info("Gracefully closing {} disconnected with BYE",ch);
							closeQuietly(ch);
						} else throw new UserManagementException("You must register before using any command other than BYE or REGISTER");
					}
					else command.execute(msg, u,ConnectionMonitor.this, sessman);
				}
			} catch (ServerException ex) {	
				log.debug("Processing error",ex);
				queueProcessingError(ch, ex);
			} catch (IndexOutOfBoundsException ex){
				log.debug("Badly formed command.");
				queueProcessingError(ch, new ProtocolSyntaxException("Badly formed command",ex));
			} catch (Exception e) {
				log.error("Failed to process command.",e);
				queueProcessingError(ch, new ServerBackendException(e));
			}
		}

	}
	private MessageHandler ensureHandler(SocketChannel ch) throws CancelledKeyException {
		// first try the user store for a handler
		MessageHandler h = store.getOutbox(ch);
		//check anonymous handlers next
		if(h==null) h = strayHandlers.get(ch);
		if(h==null) {
			SelectionKey key = ch.keyFor(selector);
			if(key == null) throw new CancelledKeyException();
			h = new MessageHandler(key);
			strayHandlers.put(ch,h);
		}
		return h;
	}
	private void queueMessage(SocketChannel ch, NetworkMessage message) {
		try {
			ensureHandler(ch).enqueue(message);
		} catch(CancelledKeyException | NullPointerException ex) {
			// if we get a CancelledKeyException here, this means the user has already been deregistered, and
			// ensureHandler failed to set up a handler for the connection in question. In other words, this thread attempted to
			// write a message after a remote disconnect.
			log.warn("Failed to write message, peer already disconnected.");
		} 
	}

	private void queueProcessingError(SocketChannel ch,	ServerException ex) {
		queueMessage(ch,ex.protocolMessage);
	}
	@Override
	public void close() throws IOException {
		Set<SelectionKey> keys = selector.keys();
		for(SelectionKey key : keys) {
			try {
				key.channel().close();
			} catch(IOException e) {} // letting this one slip risks a resource leak
			key.cancel();
		}
		selector.close();
	}
	@Override
	public void finalize() {
		try {
			log.warn("Mopping up unclosed connection monitor.");
			close();
		} catch (IOException e) {	}
	}

	@Override
	public void register(User user) throws UserManagementException {
		store.addUser(user);
		strayHandlers.remove(user.channel); // ensure the user's outbox is removed from the stray handler list
		// message behaviour for remaining messages in the old handler is undefined now, but this shouldn't really matter
		messageUser(user, new NetworkMessage(ClientCommandList.WELCOME,user.handle));
		log.info("Registered user {}",user);
		lobbyBroadcast(user, new NetworkMessage(ClientCommandList.SAY, String.format("User %s entered the room.",user.handle)));
	}

	@Override
	public void deregister(User user) throws UserManagementException {
		// TODO allow for disconnect handlers? 
		if(user==null) throw new UserManagementException("null is not a user");
		String username = user.handle;
		user.purgeResponseHandlers();
		boolean nosession = user.getSession() == null;
		if(!nosession) {
			try {
				sessman.removeUser(user);
			} catch (SessionException e) {
				new UserManagementException(e);
			}
		}
		store.removeUser(user);
		log.info("Deregistered user {}",user);
		closeQuietly(user.channel);
		if(nosession) lobbyBroadcast(user, new NetworkMessage(ClientCommandList.SAY,String.format("User %s has disconnected.",username)));		
	}

	private void closeQuietly(SocketChannel channel) {
		SelectionKey key = channel.keyFor(selector);
		if(key != null) key.cancel();
		try {
			channel.close();
		} catch (IOException e) {}
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
		if(handler != null)
			user.enqueueActiveResponseHandler(handler);
	}

	@Override
	public void messageUser(User user, NetworkMessage message) {
		queueMessage(user.channel, message);
	}
	private static enum AdminCommand {
		RECONF {
			@Override
			public Runnable getTask(User issuer, ConnectionMonitor mon, NetworkMessage command) {
				return mon.rereadConfig;
			}
		}, BROADCAST {
			@Override
			public Runnable getTask(final User issuer, final ConnectionMonitor mon, final NetworkMessage command) throws ArgumentCountException {
				if(command.argCount()<2) throw new ArgumentCountException(Type.TOO_FEW, BROADCAST);
				return new Runnable() {
					@Override
					public void run() {
						NetworkMessage message = new NetworkMessage(ClientCommandList.SAY,String.format("[GLOBAL from %s]\n%s",issuer.handle,command.get(1)));
						for(User u : mon.store) {
							mon.messageUser(u, message);
						}
					}
				};
			}
		}, KICK {

			@Override
			public Runnable getTask(User issuer, final ConnectionMonitor mon,
					NetworkMessage command) throws ProtocolSyntaxException {
				if(command.argCount()<2) throw new ArgumentCountException(Type.TOO_FEW, KICK);
				String username = command.get(1);
				final User toBeKicked;
				try {
					toBeKicked = mon.getUser(username);
				} catch (UserManagementException e) {
					log.warn("User {} not found, aborting.",username,e);
					return null;
				}
				return new Runnable() {

					@Override
					public void run() {
						try {
							mon.deregister(toBeKicked);
						} catch (UserManagementException e) {
							log.warn("Failed to kick user {}",toBeKicked,e);
							return;
						}
					}
					
				};
			}
			
		}, NUKESESSION {
			@Override
			public Runnable getTask(User issuer, final ConnectionMonitor mon,
					NetworkMessage command) throws ProtocolSyntaxException {
				if(command.argCount()<2) throw new ArgumentCountException(Type.TOO_FEW, NUKESESSION);
				final Session target;
				try {
					target = mon.sessman.getSession(Integer.parseInt(command.get(1)));
				} catch (Exception e) {
					log.warn("Exception in NUKESESSION prep",e);
					return null;
				}
				
				return target == null ? null : new Runnable() {
					
					@Override
					public void run() {
						mon.sessman.destroySession(target);
					}
				};
			}
		}, SHUTDOWN {

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
		public abstract Runnable getTask(User issuer, ConnectionMonitor mon, NetworkMessage command) throws ProtocolSyntaxException;
	}
	@Override
	public void adminCommand(User issuer, int id, NetworkMessage commandMessage) throws UserManagementException,ProtocolSyntaxException {
		boolean adminEnabled = config.getSafely(ConfigFields.ENABLE_ADMIN, Boolean.class,ConfigFields.ENABLE_ADMIN_DEFAULT);
		@SuppressWarnings("unchecked")
		List<String> allowedIps = config.getSafely(ConfigFields.ADMIN_WHITELIST, List.class, ConfigFields.ADMIN_WHITELIST_DEFAULT);
		if(!adminEnabled) {
			queueProcessingError(issuer.channel, new ServerException("Admin commands are disabled.", ServerException.ERROR_GENERIC));
			return;			
		}
		if(commandMessage.argCount()==0) throw new ArgumentCountException(Type.TOO_FEW, ServerCommand.ADMIN); // never hurts to be extra sure
		InetAddress addr;
		try {
			addr = ((InetSocketAddress)issuer.channel.getRemoteAddress()).getAddress();
		} catch (IOException e) {
			log.error("Failed to get command issuer address. Aborting operation.");
			return;
		}
		if(!addr.isLoopbackAddress() && !checkAddress(addr,allowedIps)) {
			// this is a weak security precaution that isn't even that hard to circumvent
			// still, admin commands should not expose anything vital
			log.warn("Warning: non-loopback address {} (bound to user {}) issued admin command. Silently ignoring.",addr,issuer.handle);
			return;
		}
		ClientResponseHandler rh;
		try {
			Runnable task = AdminCommand.valueOf(commandMessage.get(0).toUpperCase()).getTask(issuer,this, commandMessage);
			if(task == null) return;
			rh= new AdminTaskExecutor(issuer, id,task);
		} catch(IllegalArgumentException ex) {
			throw new ProtocolSyntaxException("Unknown command "+commandMessage.get(0));
		}
		messageUser(issuer,new NetworkMessage(ClientCommandList.CONFIRMADMIN,id,rh.id),rh);
	}
	private static boolean checkAddress(InetAddress addr, List<String> allowedIps) {
		for(String s : allowedIps) {
			try {
				if(InetAddress.getByName(s).equals(addr)) return true;
			} catch (UnknownHostException e) {
				log.warn("Unknown host {}",s);
				continue;
			}
		}
		return false;
	}
	@Override
	public void lobbyBroadcast(User user, NetworkMessage msg) {
		for(User u : store) {
			if(u.getSession() == null && !u.equals(user))
				messageUser(u,msg); //only message fellow users that are not in a session
		}
	}
}