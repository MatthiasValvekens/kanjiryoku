package be.mapariensis.kanjiryoku.net.model;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
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
	private SSLEngineResult sslres = null;

	public SSLMessageHandler(SelectionKey key, SSLEngine engine,
			ExecutorService delegatedTaskPool, int plaintextBufsize) {
		super(key);
		int netbufsize = engine.getSession().getPacketBufferSize();
		log.trace("Buffers initialised to: net: {}, app: {}", netbufsize,
				plaintextBufsize);
		this.netIn = ByteBuffer.allocate(netbufsize);
		this.appIn = ByteBuffer.allocate(plaintextBufsize + 256);
		this.appOut = ByteBuffer.allocate(plaintextBufsize);
		this.netOut = ByteBuffer.allocate(netbufsize);
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
	public void flushMessageQueue() throws IOException {
		try {
			flush();
			sslWrite();
			log.trace("After write: ops: {} netOut pos: {} appOut pos: {}",
					key.interestOps(), netOut.position(), appOut.position());
			// unset OP_WRITE in case the handshake() method
			// registered for it after noticing no handshaking was being done.
			if (!needSend()) {
				synchronized (key) {
					key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
				}
			}
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
		if (!sslRead())
			return Collections.emptyList();
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

	private boolean sslRead() throws IOException {
		int bytesRead;
		ReadableByteChannel ch = (ReadableByteChannel) key.channel();
		bytesRead = ch.read(netIn);
		if (bytesRead == -1)
			throw new EOFException();
		if (bytesRead == 0)
			return false;

		// decrypt data
		netIn.flip();
		sslres = engine.unwrap(netIn, appIn);
		netIn.compact(); // safeguard against partial reads
		switch (sslres.getStatus()) {
		case BUFFER_UNDERFLOW:
			return false;
		case BUFFER_OVERFLOW:
			throw new BufferOverflowException();
		case CLOSED:
			((SocketChannel) key.channel()).socket().shutdownInput();
			break;
		case OK:
			break;
		}

		//noinspection StatementWithEmptyBody
		while (handshake());

		// EOF conditions
		return !engine.isInboundDone();
	}


	private void sslWrite() throws IOException {
		appOut.flip();
		sslres = engine.wrap(appOut, netOut);
		appOut.compact();

		switch (sslres.getStatus()) {
		case BUFFER_UNDERFLOW:
			throw new BufferUnderflowException();
		case BUFFER_OVERFLOW:
			throw new BufferOverflowException();
		case CLOSED:
			throw new SSLException("SSL engine closed.");
		case OK:
			break;
		}

		//noinspection StatementWithEmptyBody
		while (handshake());
		flush();
	}

	private boolean handshake() throws IOException {
		SocketChannel ch = (SocketChannel) key.channel();
		log.trace("HS: {}", engine.getHandshakeStatus());
		switch (engine.getHandshakeStatus()) {
		case NEED_TASK:
			if (!requestedTaskExecution) {
				delegatedTaskPool.execute(new DelegatedTaskWorker());
				requestedTaskExecution = true;
			}
			return false;
		case NEED_UNWRAP:
			if (!engine.isInboundDone()) {
				ch.read(netIn);
			}
			netIn.flip();
			log.trace("Unwrapping...");
			sslres = engine.unwrap(netIn, appIn);
			log.trace("Unwrapping done.");
			netIn.compact();
			break;
		case NEED_WRAP:
			appOut.flip();
			flush();
			log.trace("Wrapping...");
			sslres = engine.wrap(appOut, netOut);
			log.trace("Wrapping done.");
			appOut.compact();
			if (sslres.getStatus() == SSLEngineResult.Status.CLOSED) {
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
			synchronized (key) {
				key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
			}
			return false;
		}

		log.trace("SR: {}", sslres.getStatus());
		switch (sslres.getStatus()) {
		case BUFFER_UNDERFLOW:
			key.interestOps(SelectionKey.OP_READ);
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
		@Override
		public void run() {
			log.trace("Running delegated task...");
			// set interestOps to clear read and write
			key.interestOps(0);

			Runnable task;
			while ((task = engine.getDelegatedTask()) != null) {
				task.run();
			}

			// restore ops, the SSL engine might want to read/write something
			// after this.
			// Calling the handshake method from this thread would be dangerous.
			synchronized (key) {
				key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			}
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
