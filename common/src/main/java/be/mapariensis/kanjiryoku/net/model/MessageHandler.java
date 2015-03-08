package be.mapariensis.kanjiryoku.net.model;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;

// The SSL support draws heavily upon Chapter 8 of "Fundamental Networking in Java"
public class MessageHandler implements Runnable, Closeable {
	private static final Logger log = LoggerFactory
			.getLogger(MessageHandler.class);
	private final Queue<byte[]> messages = new ConcurrentLinkedQueue<byte[]>();
	private volatile boolean sendingNow = false;
	private final SelectionKey key;
	private static final byte[] GOODBYE = ServerCommandList.BYE.toString()
			.getBytes();
	private final ByteBuffer netIn, appIn, appOut, netOut;
	private final SSLEngine engine;
	private final ExecutorService delegatedTaskPool;
	private SSLEngineResult sslres = null;

	public MessageHandler(SelectionKey key, int bufsize, SSLEngine engine,
			ExecutorService delegatedTaskPool) {
		if (key == null)
			throw new IllegalArgumentException();
		this.key = key;
		this.netIn = ByteBuffer.allocate(bufsize);
		this.appIn = ByteBuffer.allocate(bufsize + 32);
		this.appOut = ByteBuffer.allocate(bufsize + 32);
		this.netOut = ByteBuffer.allocate(bufsize);
		this.engine = engine;
		this.delegatedTaskPool = delegatedTaskPool;
	}

	public void enqueue(NetworkMessage message) {
		if (message == null || message.isEmpty())
			return;
		enqueue(message.toString().getBytes(Constants.ENCODING));
	}

	protected void enqueue(byte[] bytes) {
		synchronized (key) {
			messages.add(bytes);
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
			key.selector().wakeup();
		}
	}

	public boolean isEmpty() {
		return messages.isEmpty();
	}

	public SSLEngine getSSLEngine() {
		return engine;
	}

	@Override
	public void run() { // only allow one sender per socket at a time
		if (messages.isEmpty() || sendingNow)
			return;
		synchronized (key) {
			sendingNow = true;
			if (key.isValid() && key.isWritable()) {
				try {
					while (!messages.isEmpty()) {
						byte[] msg = messages.poll();
						sendMessageNow(msg);
					}
				} catch (IOException e) {
					log.error("I/O failure while sending messages", e);
				} finally {
					sendingNow = false;
					key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
				}
			} else {
				log.error(
						"Channel no longer available for writing. Validity {}, writability {}.",
						key.isValid(), key.isWritable());
				sendingNow = false;
			}
		}
		key.selector().wakeup();
	}

	private void sendMessageNow(byte[] message) throws IOException {
		for (byte b : message) {
			if (b == NetworkMessage.EOM)
				throw new IOException("EOM byte is illegal in messages.");
		}
		appOut.clear();
		appOut.put(message);
		appOut.put(NetworkMessage.EOM);
		appOut.flip();

		sslWrite();

		flush();
	}

	private final CharsetDecoder decoder = Constants.ENCODING.newDecoder();

	public List<NetworkMessage> readRaw() throws IOException, EOFException {
		synchronized (key) {
			if (engine.isInboundDone())
				return Collections.emptyList();
			// read encrypted data
			// and return the empty list if nothing particularly interesting
			// was found
			if (!sslRead())
				return Collections.emptyList();

			// process decrypted data
			// need to remember this for later
			int finalPosition = appIn.position();
			appIn.limit(finalPosition);
			// search backwards until we find the first EOM
			int lastEom;
			for (lastEom = appIn.limit() - 1; lastEom >= 0; lastEom--) {
				appIn.position(lastEom);
				if (appIn.get() == NetworkMessage.EOM)
					break;
			}
			// no full message received
			// the position is now at 0
			if (lastEom == -1) {
				appIn.compact();
				return Collections.emptyList();
			}
			// the position is one byte after the last EOM
			// so we can flip, and compact in the end
			appIn.flip();

			// decode the input into network messages
			CharBuffer decodedInput = CharBuffer.allocate(appIn.limit());
			// there should not be any incomplete characters,
			// after all, we cut off at the last EOM
			decoder.decode(appIn, decodedInput, true);
			decoder.flush(decodedInput);
			decodedInput.flip();
			List<NetworkMessage> result = new ArrayList<NetworkMessage>();
			while (decodedInput.position() < decodedInput.limit()) {
				// buildArgs stops when the limit is reached, or when it
				// reaches EOM
				result.add(NetworkMessage.buildArgs(decodedInput));
			}
			decoder.reset();

			// prepare netIn buffer for next read
			appIn.limit(finalPosition);
			appIn.compact();

			return result;
		}
	}

	@Override
	public void close() throws IOException {
		// negotiate end of SSL connection
		synchronized (key) {
			sendMessageNow(GOODBYE);
			if (!engine.isOutboundDone()) {
				engine.closeOutbound();
				while (handshake())
					;
			} else if (!engine.isInboundDone()) {
				engine.closeInbound();
				handshake();
			}
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

		while (handshake())
			;

		// EOF conditions
		if (bytesRead == -1)
			engine.closeInbound();
		if (engine.isInboundDone())
			return false;
		return true;
	}

	private void flush() throws IOException {
		netOut.flip();
		WritableByteChannel ch = (WritableByteChannel) key.channel();
		synchronized (key) {
			ch.write(netOut);
		}
		netOut.compact();
	}

	private void sslWrite() throws IOException {
		netOut.clear();
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

		while (handshake())
			;
	}

	private boolean handshake() throws IOException {
		SocketChannel ch = (SocketChannel) key.channel();
		switch (engine.getHandshakeStatus()) {
		case NEED_TASK:
			delegatedTaskPool.execute(new DelegatedTaskWorker());
			return false;
		case NEED_UNWRAP:
			if (!engine.isInboundDone()) {
				ch.read(netIn);
			}
			netIn.flip();
			sslres = engine.unwrap(netIn, appIn);
			netIn.compact();
			break;
		case NEED_WRAP:
			appOut.flip();
			sslres = engine.wrap(appOut, netOut);
			appOut.compact();
			if (sslres.getStatus() == SSLEngineResult.Status.CLOSED) {
				try {
					flush();
				} catch (SocketException ex) {
					log.info("Peer closed socked without waiting for close_notify.");
				}
			} else {
				// exceptions should be allowed to propagate.
				flush();
			}
			break;
		case FINISHED:
		case NOT_HANDSHAKING:
			return false;
		}

		switch (sslres.getStatus()) {
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

	private class DelegatedTaskWorker implements Runnable {
		@Override
		public void run() {
			// set interestOps to clear read and write
			int oldops = key.interestOps();
			key.interestOps(0);
			Runnable task;
			while ((task = engine.getDelegatedTask()) != null) {
				task.run();
			}
			// restore ops
			key.interestOps(oldops);
		}
	}
}
