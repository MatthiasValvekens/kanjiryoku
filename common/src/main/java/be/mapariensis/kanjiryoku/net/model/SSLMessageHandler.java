package be.mapariensis.kanjiryoku.net.model;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.commands.ClientCommandList;

// The SSL support draws heavily upon Chapter 8 of "Fundamental Networking in Java"
public class SSLMessageHandler extends MessageHandler {
	private static final Logger log = LoggerFactory
			.getLogger(SSLMessageHandler.class);
	private volatile boolean requestedTaskExecution = false;
	private final Object APPOUT_LOCK = new Object();
	private final ByteBuffer netIn, appIn, appOut, netOut;
	private final SSLEngine engine;
	private final ExecutorService delegatedTaskPool;
	// This needs to be shared to simplify the handshake() state operations
	private SSLEngineResult sslRes = null;

	public SSLMessageHandler(SelectionKey key, SSLEngine engine,
			ExecutorService delegatedTaskPool, int plaintextBufsize) {
		super(key);
		int netbufsize = engine.getSession().getPacketBufferSize();
		plaintextBufsize = Math.max(plaintextBufsize, engine.getSession().getApplicationBufferSize());
		log.trace("Buffers initialised to: net: {}, app: {}", netbufsize,
				plaintextBufsize);
		this.netIn = ByteBuffer.allocate(netbufsize);
		this.netOut = ByteBuffer.allocate(netbufsize);
		this.appIn = ByteBuffer.allocate(plaintextBufsize);
		this.appOut = ByteBuffer.allocate(plaintextBufsize);
		this.engine = engine;
		this.delegatedTaskPool = delegatedTaskPool;
	}

	/**
	 * Enqueue a message without flushing the message buffer
	 * 
	 * @param message
	 *   Message to put in the queue.
	 */
	@Override
	public void enqueue(NetworkMessage message) {
		if (message == null || message.isEmpty())
			return;
		enqueue(message.toString().getBytes(Constants.ENCODING));
	}

	protected void enqueue(byte[] message) {
		for (byte b : message) {
			if (b == NetworkMessage.EOM)
				throw new RuntimeException("EOM byte is illegal in messages.");
		}
		synchronized (APPOUT_LOCK) {
			appOut.put(message);
			appOut.put(NetworkMessage.EOM);
		}
	}

	/**
	 * Buffer a message and flush the buffer while holding the lock on this
	 * handler's application output buffer.
	 *
	 * @param message
	 *   Message to send.
	 * @throws IOException
	 *   Thrown if the underlying {@link java.nio.ByteBuffer#put(byte[]) put} method fails.
	 */
	@Override
	public void send(NetworkMessage message) throws IOException {
		if (message == null || message.isEmpty())
			return;
		log.trace("About to send {}", message);
		send(message.toString().getBytes(Constants.ENCODING));
	}

	protected void send(byte[] message) throws IOException {
		for (byte b : message) {
			if (b == NetworkMessage.EOM)
				throw new RuntimeException("EOM byte is illegal in messages.");
		}
		synchronized (APPOUT_LOCK) {
			appOut.put(message);
			appOut.put(NetworkMessage.EOM);
			flushMessageQueue();
		}
	}

	public SSLEngine getSSLEngine() {
		return engine;
	}

	@Override
	public int flushMessageQueue() throws IOException {
		try {
			flush();
			int bytesWritten = sslWrite();
			log.trace("After write: ops: {} netOut pos: {} appOut pos: {}",
					key.interestOps(), netOut.position(), appOut.position());
			// unset OP_WRITE in case the handshake() method
			// registered for it after noticing no handshaking was being done.
			if (!needSend()) {
				key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
			}
			return bytesWritten;
		} catch (BufferOverflowException | BufferUnderflowException e) {
			throw new IOException(e);
		}
	}

	@Override
	protected ByteBuffer getNetworkOutputBuffer() {
		return netOut;
	}

	@Override
	protected ByteBuffer getApplicationInputBuffer() {
		return appIn;
	}

	@Override
	public List<NetworkMessage> readRaw() throws IOException {
		if (engine.isInboundDone())
			throw new EOFException();
		// read encrypted data
		// and return the empty list if nothing particularly interesting
		// was found
        int bytesRead = sslRead();
        if (bytesRead == -1)
			throw new EOFException();
		if (bytesRead == 0) {
			return Collections.emptyList();
		}
		// appIn is now prepared for a read
		return super.readRaw();
	}

	@Override
	public void close() throws IOException {
		try {
			// negotiate end of SSL connection
			if (!engine.isOutboundDone()) {
				engine.closeOutbound();
				//noinspection StatementWithEmptyBody
				while (handshake());
			} else if (!engine.isInboundDone()) {
				engine.closeInbound();
				handshake();
			}
		} finally {
			key.channel().close();
			key.cancel();
		}
	}

	private int sslRead() throws IOException {
		if(engine.isInboundDone())
			return -1;

		int initialPosition = appIn.position();
		int bytesRead;
		SocketChannel ch = (SocketChannel) key.channel();
		bytesRead = ch.read(netIn);

		// decrypt data
		netIn.flip();
		boolean keepReading = true;
		while (keepReading) {
			keepReading = false;
			sslRes = engine.unwrap(netIn, appIn);
			switch (sslRes.getStatus()) {
				case BUFFER_UNDERFLOW:
					return 0;
				case BUFFER_OVERFLOW:
				    // partial read safeguard?
					// TODO is this necessary?
					netIn.compact();
					throw new BufferOverflowException();
				case CLOSED:
					ch.socket().shutdownInput();
					break;
				case OK:
				    keepReading = netIn.hasRemaining();
					break;
			}
			// unwrap only decrypts one packet at a time, so
			// we need to keep on keeping on until the buffer
			// is empty (and we're not eating the engine's data / partial packets)
            keepReading &= sslRes.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
		}
		netIn.compact();

		log.trace("Post-read handshake handling...");
		//noinspection StatementWithEmptyBody
		while (handshake());

		// EOF conditions
		if (bytesRead == -1) {
		    engine.closeInbound();
		}
		if(engine.isInboundDone())
			return -1;
		// return bytes of application data read
		return appIn.position() - initialPosition;
	}


	private int sslWrite() throws IOException {
		int initialPosition = appOut.position();
		appOut.flip();
		sslRes = engine.wrap(appOut, netOut);
		appOut.compact();

		switch (sslRes.getStatus()) {
		case BUFFER_UNDERFLOW:
			throw new BufferUnderflowException();
		case BUFFER_OVERFLOW:
			throw new BufferOverflowException();
		case CLOSED:
			throw new SSLException("SSL engine closed.");
		case OK:
			break;
		}

		log.trace("Post-write handshake handling...");
		//noinspection StatementWithEmptyBody
		while (handshake());
		flush();
		return appOut.position() - initialPosition;
	}

	private boolean handshake() throws IOException {
		SocketChannel ch = (SocketChannel) key.channel();
		log.trace("HS: {}", engine.getHandshakeStatus());
		switch (engine.getHandshakeStatus()) {
		case NEED_TASK:
		    // Using AtomicBoolean here is not necessary, since the message handlers
			//  are supposed to be called by the server/client monitor thread only.
            // If requestedTaskExecution is false going into this branch, the comparison
			//  will therefore not be executed by any other thread.
			if (!requestedTaskExecution) {
				requestedTaskExecution = true;
				//int ops = key.interestOps();
				// prevent reads and writes while we wait for
				//  the delegated task to complete.
				// The worker will restore the ops when it's done
				key.interestOps(0);
				delegatedTaskPool.execute(new DelegatedTaskWorker(SelectionKey.OP_READ | SelectionKey.OP_WRITE));
			}
			return false;
		case NEED_UNWRAP:
			if (!engine.isInboundDone()) {
				ch.read(netIn);
			}
			netIn.flip();
			log.trace("Unwrapping...");
			sslRes = engine.unwrap(netIn, appIn);
			log.trace("Unwrapping done.");
			netIn.compact();
			break;
		case NEED_WRAP:
			appOut.flip();
			flush();
			log.trace("Wrapping...");
			sslRes = engine.wrap(appOut, netOut);
			log.trace("Wrapping done.");
			appOut.compact();
			if (sslRes.getStatus() == SSLEngineResult.Status.CLOSED) {
				try {
					flush();
				} catch (SocketException ex) {
					log.trace("Peer closed socked without waiting for close_notify.");
				}
			} else {
				log.trace("Flushing after wrap");
				// exceptions should be allowed to propagate.
				flush();
			}
			break;
		case FINISHED:
		case NOT_HANDSHAKING:
			// register for OP_WRITE
			// The reason we do this here is because
			// IF there's any data left in the application buffer,
			// the application should register for OP_WRITE to finish its write
			// However, when this is done while the SSL engine is waiting for
			// handshake data
			// this wastes CPU cycles by attempting (and failing) to move data
			// from appOut to netOut

			// Should the client not have anything to write, the next call to
			// flushMessageQueue() will set this straight
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
			return false;
		}

		log.trace("SR: {}", sslRes.getStatus());
		switch (sslRes.getStatus()) {
		case BUFFER_UNDERFLOW:
		case BUFFER_OVERFLOW:
			return false;
		case CLOSED:
			if (engine.isOutboundDone()) {
				ch.socket().shutdownOutput();
			}
			return false;
		case OK:
			break;
		}
		return true;
	}

	@Override
	public boolean needSend() {
		return netOut.position() > 0 || appOut.position() > 0;
	}

	private class DelegatedTaskWorker implements Runnable {
		final int originalOps;

		DelegatedTaskWorker(int originalOps) {
			this.originalOps = originalOps;
		}

		@Override
		public void run() {
			log.trace("Running delegated task...");
			Runnable task;
			while ((task = engine.getDelegatedTask()) != null) {
				task.run();
			}

			// restore ops, the SSL engine might want to read/write something
			// after this.
			// Calling the handshake method from this thread would be dangerous.
			key.interestOps(originalOps);
			requestedTaskExecution = false;
			log.trace("Task finished");
		}
	}

	@Override
	public void dispose() {
		disposed = true;
	}

	@Override
	public void dispose(String disconnectMessage) {
		dispose(new NetworkMessage(ClientCommandList.SAY, disconnectMessage));
	}

	@Override
	public void dispose(NetworkMessage disconnectMessage) {
		disposed = true;
		try {
			send(disconnectMessage);
		} catch (IOException e) {
			log.warn("I/O error during dispose.", e);
		}
	}
}
