package be.mapariensis.kanjiryoku.net.client;

import static be.mapariensis.kanjiryoku.net.Constants.BUFFER_MAX;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.gui.GUIBridge;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.model.ClientCommand;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.ServerCommand;
import be.mapariensis.kanjiryoku.net.server.MessageHandler;
import be.mapariensis.kanjiryoku.net.util.NetworkThreadFactory;

public class ServerUplink extends Thread implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(ServerUplink.class);
	private static final int WORKER_THREADS = 10;
	private static final long SELECT_TIMEOUT = 500;
	private final ExecutorService threadPool;
	private final SocketChannel channel;
	private volatile boolean keepOn = true;
	private boolean registerAttempted = false; // marks whether the client has attempted to register with the server
	private boolean registerCompleted = false;
	private final Selector selector;
	private final InetAddress addr;
	private final int port;
	private final String username;
	private MessageHandler messageHandler;
	private SelectionKey key;
	private final GUIBridge bridge;
	public ServerUplink(InetAddress addr, int port, String username, GUIBridge bridge) throws IOException {
		channel = SocketChannel.open();
		selector = Selector.open();
		this.addr = addr;
		this.port = port;
		this.bridge = bridge;
		this.username = username;
		threadPool = Executors.newFixedThreadPool(WORKER_THREADS, new NetworkThreadFactory(BUFFER_MAX,selector));
	}
	
	@Override
	public void run() {
		// block until channel connects, and then switch to nonblocking mode
		try {
			channel.configureBlocking(true);
			channel.connect(new InetSocketAddress(addr, port));
			channel.configureBlocking(false);
			key = channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			messageHandler = new MessageHandler(key);
		} catch (IOException e) {
			log.error("Failed to connect.",e);
			try {
				close();
			} catch (IOException e1) {}
			bridge.getChat().displaySystemMessage("Could not connect to server.");
			return;
		}
		ByteBuffer messageBuffer = ByteBuffer.allocateDirect(BUFFER_MAX);
		while(keepOn) {
			int readyCount;
			try {
				readyCount = selector.select(SELECT_TIMEOUT);
			} catch (IOException e) {
				log.error("I/O error during selection",e);
				break;
			}
			if(readyCount == 0) continue;
			
			// clear event
			selector.selectedKeys().remove(key);	
			
			if(!key.isValid()) {
				log.error("Key no longer valid");
				break; // TODO : handle this robustly
			}
			if(key.isReadable()) {
				final List<NetworkMessage> msgs;
				try {
					msgs = NetworkMessage.readRaw(channel, messageBuffer);
				} catch(IOException ex) {  //FIXME : figure out a way to deal with forcefully closed connections, and then downgrade this to EOFException
					key.cancel();
					try {
						log.info("Peer {} shut down.", channel.getRemoteAddress());							
						close();
						break;
					} catch(IOException e) {
						break;
					}
				}
				for(final NetworkMessage msg : msgs) {
					if(msg.isEmpty()) continue;
					// check for server post-registration message
					if(msg.get(0).equals(ClientCommand.WELCOME.toString())) {
						log.info("Registration complete");
						registerCompleted = true;
					}
					if(!registerCompleted) {
						threadPool.execute(new Runnable() {
							@Override
							public void run() {
								bridge.getChat().displayServerMessage(msg.toString());
							}
						});
					} else if(!msg.isEmpty()) threadPool.execute(new CommandReceiver(msg)); // schedule command interpretation
				}
			}

			if(key.isWritable()) {
				if(!registerAttempted) {
					enqueueMessage(ServerCommand.REGISTER,username);
					registerAttempted = true;
				}
				threadPool.execute(messageHandler);
			}
		}
	}
	private class CommandReceiver implements Runnable {
		private final NetworkMessage msg;
		private CommandReceiver(NetworkMessage msg) {
			this.msg = msg;
		}
		@Override
		public void run() {
			if(msg.argCount()==0) return;
			String cmdstring = msg.get(0);
			if(ServerException.isError(cmdstring)) {
				try {
					bridge.getChat().displayErrorMessage(ServerException.parseErrorCode(cmdstring), msg.get(1));// TODO better exception handling
				} catch (ParseException e) {
				}
				return;
			}
			ClientCommand command;
			try {
				command = ClientCommand.valueOf(cmdstring);
			} catch (IllegalArgumentException ex) {
				bridge.getChat().displayErrorMessage(new ServerCommunicationException(ex));
				return;
			}
			try {
				command.execute(msg, bridge.getClient(), bridge.getChat()); // FIXME get gci from somewhere
			} catch (ClientException e) {
				serverError(e);
			}
		}

	}
	private void serverError(ClientException ex) {
		bridge.getChat().displayErrorMessage(ex.errorCode, ex.getMessage());
		log.error("Error while parsing server message",ex);
	}
	public void enqueueMessage(String message) {
		messageHandler.enqueue(message);
	}
	public void enqueueMessage(Object... args) {
		messageHandler.enqueue(new NetworkMessage(args).toString());
	}
	public void enqueueMessage(NetworkMessage msg) {
		enqueueMessage(msg.toString());
	}
	@Override
	public void close() throws IOException {
		selector.close();
		channel.close();
	}
	public String getUsername(){
		return username;
	}
	void stopListening() {
		log.info("Received listener shutdown request.");
		keepOn = false;
	}
}
