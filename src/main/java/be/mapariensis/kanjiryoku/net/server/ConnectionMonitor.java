package be.mapariensis.kanjiryoku.net.server;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static be.mapariensis.kanjiryoku.net.Constants.*;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.client.ClientCommand;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.ServerCommand;
import be.mapariensis.kanjiryoku.net.model.ResponseHandler;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.model.UserStore;
import be.mapariensis.kanjiryoku.net.util.NetworkThreadFactory;
import be.mapariensis.kanjiryoku.util.Filter;

public class ConnectionMonitor extends Thread implements UserManager, Closeable {
	private static final Logger log = LoggerFactory.getLogger(ConnectionMonitor.class);
	private static final int WORKER_THREADS = 10;
	private final ExecutorService threadPool = Executors.newFixedThreadPool(WORKER_THREADS, new NetworkThreadFactory(BUFFER_MAX));
	private volatile boolean keepOn = true;
	private final ServerSocketChannel ssc;
	private final Selector selector;
	private final UserStore store = new UserStore();
	private final SessionManagerImpl sessman = new SessionManagerImpl(this);
	// store message handlers for anonymous connections here until they register/identify
	private final Map<SocketChannel,MessageHandler> strayHandlers = new ConcurrentHashMap<SocketChannel,MessageHandler>();

	public ConnectionMonitor(ServerSocketChannel ssc) throws IOException {
		this.ssc = ssc;
		selector = Selector.open();
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
			return;
		}
		ByteBuffer messageBuffer = ByteBuffer.allocateDirect(BUFFER_MAX); // allocate one buffer for the monitor thread
		while(keepOn) {
			int readyCount;
			try {
				readyCount = selector.select();
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
						ch.register(selector, SelectionKey.OP_READ);
						queueMessage(ch, GREETING);
					} else if(key.isReadable()) {
						ch = (SocketChannel) key.channel();
						NetworkMessage msg;
						try {
							msg = NetworkMessage.readRaw(ch, messageBuffer);
						} catch(IOException ex) {  //FIXME : figure out a way to deal with forcefully closed connections, and then downgrade this to EOFException
							log.info("Peer {} shut down.", ch.getRemoteAddress());
							key.cancel();
							User u;
							if((u = store.getUser(ch))!= null) {
								deregister(u);
							}
							continue;
						} 
						// schedule command interpretation
						if(msg != null && !msg.isEmpty()) threadPool.execute(new CommandReceiver(ch, msg));
					} else if(key.isWritable()) {
						ch = (SocketChannel) key.channel();
						// deregister write event
						synchronized(key) {
							key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
						}
						threadPool.execute(ensureHandler(ch));
					}
				} catch(Exception ex) {
					log.warn("Error while processing {}. Ignoring.",ch != null ? ch : "(unknown address)",ex);
				}
			}

		}
		log.info("Connection listener shut down.");
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
				try {
					command = ServerCommand.valueOf(msg.get(0).toUpperCase());
				} catch (IllegalArgumentException ex) {
					throw new ProtocolSyntaxException();
				}
				// check for REGISTER command (which gets special treatment)
				if(command == ServerCommand.REGISTER) {
					String handle = msg.get(1);
					register(new User(handle,ch,new MessageHandler(ch.keyFor(selector))));
				} else {
					User u = store.getUser(ch);
					if(u == null) {
						if(command == ServerCommand.BYE)
							closeQuietly(ch);
						else throw new UserManagementException("You must register before using any command other than BYE or REGISTER");
					}
					else command.execute(msg, u,ConnectionMonitor.this, sessman);
				}
			} catch (ServerException ex) {
				log.warn("Processing error",ex);
				queueProcessingError(ch, ex);
			} catch (Exception e) {
				log.error("Failed to process command.",e);
				try {
					ch.close();
				} catch (IOException e1) { }
			}
		}

	}
	private MessageHandler ensureHandler(SocketChannel ch) {
		// first try the user store for a handler
		MessageHandler h = store.getOutbox(ch);
		//check anonymous handlers next
		if(h==null) h = strayHandlers.get(ch);
		if(h==null) {
			h = new MessageHandler(ch.keyFor(selector));
			strayHandlers.put(ch,h);
		}
		return h;
	}
	private void queueMessage(SocketChannel ch, String message) {
		ensureHandler(ch).enqueue(message);
	}

	private void queueProcessingError(SocketChannel ch,	ServerException ex) {
		queueMessage(ch,ex.getMessage());
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
		messageUser(user,"Welcome");
		log.info("Registered user {}",user);
	}

	@Override
	public void deregister(User user) throws UserManagementException {
		if(user==null) throw new UserManagementException("null is not a user");
		try {
			sessman.removeUser(user);
		} catch (SessionException e) {
			new UserManagementException(e);
		}
		store.removeUser(user);
		log.info("Deregistered user {}",user);
		closeQuietly(user.channel);
	}

	private void closeQuietly(SocketChannel channel) {
		log.info("Gracefully closing {} disconnected with BYE",channel);
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
	public void messageUser(User user, String message) {
		queueMessage(user.channel, message);
	}
	@Override
	public void messageUser(User user, String message, ResponseHandler handler) {
		messageUser(user, message);
		user.enqueueActiveResponseHandler(handler);
	}
	@Override
	public void broadcastMessage(String message) {
		// TODO Auto-generated method stub

	}
	@Override
	public void broadcastMessage(String message, Filter<User> filter) {
		// TODO Auto-generated method stub

	}

	@Override
	public void humanMessage(User user, String message) {
		messageUser(user, new NetworkMessage(ClientCommand.SAY, Arrays.asList(message)));

	}

	@Override
	public void messageUser(User user, NetworkMessage message,
			ResponseHandler handler) {
		messageUser(user, message.toString(),handler);		
	}

	@Override
	public void messageUser(User user, NetworkMessage message) {
		messageUser(user, message.toString());

	}


}