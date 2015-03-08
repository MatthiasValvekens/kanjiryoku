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
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.Constants;

// The SSL support draws heavily upon Chapter 8 of "Fundamental Networking in Java"
public class MessageHandler implements Closeable {
	private static final Logger log = LoggerFactory
			.getLogger(MessageHandler.class);
	private volatile boolean requestedTaskExecution = false;
	private final Object APPOUT_LOCK = new Object();
	private final SelectionKey key;
	private final ByteBuffer netIn, appIn, appOut, netOut;
	private final SSLEngine engine;
	private final ExecutorService delegatedTaskPool;
	private SSLEngineResult sslres = null;

	public MessageHandler(SelectionKey key, SSLEngine engine,
			ExecutorService delegatedTaskPool) {
		if (key == null)
			throw new IllegalArgumentException();
		this.key = key;
		int netbufsize = engine.getSession().getPacketBufferSize();
		int appbufsize = engine.getSession().getApplicationBufferSize();
		log.debug("Buffers initialised to: net: {}, app: {}", netbufsize,
				appbufsize);
		this.netIn = ByteBuffer.allocate(netbufsize);
		this.appIn = ByteBuffer.allocate(appbufsize + 256);
		this.appOut = ByteBuffer.allocate(appbufsize);
		this.netOut = ByteBuffer.allocate(netbufsize);
		this.engine = engine;
		this.delegatedTaskPool = delegatedTaskPool;
	}

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
		key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
		key.selector().wakeup();

	}

	public SSLEngine getSSLEngine() {
		return engine;
	}

	public void flushMessageQueue() {
		// FIXME: Thread-safety: this method might be sending stale data
		// which should be OK, but it's bad practice still.
		// Can't fix until the task flushing the message queue is moved to a
		// separate thread (i.e. not the connection monitor, which can't afford
		// to block).
		try {
			flush();
			if (!needSend())
				return;
			sslWrite();
			log.debug("SSL write operation done");
		} catch (IOException e) {
			log.error("I/O failure while sending messages", e);
		}

		key.selector().wakeup();
	}

	private final CharsetDecoder decoder = Constants.ENCODING.newDecoder();

	public List<NetworkMessage> readRaw() throws IOException, EOFException {
		if (engine.isInboundDone())
			return Collections.emptyList();
		appIn.clear(); // FIXME: does this discard anything significant?
		// read encrypted data
		// and return the empty list if nothing particularly interesting
		// was found
		if (!sslRead())
			return Collections.emptyList();

		// process decrypted data
		// need to remember this for later
		int finalPosition = appIn.position();
		log.info("JKJLD {}", finalPosition);
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
		// prepare netIn buffer for next read
		appIn.limit(finalPosition);
		appIn.compact();

		decodedInput.flip();
		List<NetworkMessage> result = new ArrayList<NetworkMessage>();

		while (decodedInput.position() < decodedInput.limit()) {
			// buildArgs stops when the limit is reached, or when it
			// reaches EOM
			result.add(NetworkMessage.buildArgs(decodedInput));
		}
		decoder.reset();
		log.debug("remaining appin bytes after rawread: {}", appIn.remaining());
		return result;
	}

	@Override
	public void close() throws IOException {
		try {
			// negotiate end of SSL connection
			if (!engine.isOutboundDone()) {
				engine.closeOutbound();
				while (handshake())
					;
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
		log.debug("kdjlfksdjlksd {} {} {} {}", appIn.position(), appIn.limit(),
				appIn.remaining(), bytesRead);
		sslres = engine.unwrap(netIn, appIn);
		netIn.compact(); // safeguard against partial reads
		log.debug("kdjlfksdjlksd {} {} {}", sslres.bytesProduced(),
				appIn.position(), netIn.position());
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
		if (netOut.position() == 0) {
			log.debug("Nothing to flush");
			return;
		}
		netOut.flip();
		SocketChannel ch = (SocketChannel) key.channel();
		log.debug("Will attempt to write {} bytes. Writability {}.",
				netOut.limit(), key.isWritable());
		ch.write(netOut);
		netOut.compact();
		if (netOut.position() > 0) {
			// short write
			log.debug("Short write! {} bytes left.", netOut.position());
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
		}
	}

	private void sslWrite() throws IOException {
		// netOut.clear();
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

		while (handshake())
			;
		flush();
	}

	private boolean handshake() throws IOException {
		log.debug("Handshake!");
		SocketChannel ch = (SocketChannel) key.channel();
		log.debug("HS: {}", engine.getHandshakeStatus());
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
			log.debug("Unwrapping...");
			sslres = engine.unwrap(netIn, appIn);
			log.debug("Unwrapping done.");
			netIn.compact();
			break;
		case NEED_WRAP:
			appOut.flip();
			flush();
			log.debug("Wrapping...");
			log.debug("Shit in netout: {} {}", netOut.position(),
					netOut.limit());
			sslres = engine.wrap(appOut, netOut);
			log.debug("Shit in netout: {} {}", netOut.position(),
					netOut.limit());
			log.debug("Wrapping done.");
			appOut.compact();
			if (sslres.getStatus() == SSLEngineResult.Status.CLOSED) {
				try {
					flush();
				} catch (SocketException ex) {
					log.info("Peer closed socked without waiting for close_notify.");
				}
			} else {
				log.debug("Flushing after wrap");
				// exceptions should be allowed to propagate.
				flush();
			}
			break;
		case FINISHED:
			log.debug("Handshake complete.");
		case NOT_HANDSHAKING:
			return false;
		}

		log.debug("SR: {}", sslres.getStatus());
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
			log.debug("Running delegated task...");
			// set interestOps to clear read and write
			int oldops;
			oldops = key.interestOps();
			key.interestOps(0);

			Runnable task;
			while ((task = engine.getDelegatedTask()) != null) {
				task.run();
			}

			// restore ops
			key.interestOps(oldops | SelectionKey.OP_WRITE);
			key.selector().wakeup();
			requestedTaskExecution = false;
			log.debug("Task finished");
		}
	}

	public boolean needSend() {
		return netOut.position() > 0 || appOut.position() > 0;
	}
}
