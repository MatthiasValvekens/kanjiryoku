package be.mapariensis.kanjiryoku.net.model;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.commands.ClientCommandList;

public class PlainMessageHandler implements IMessageHandler {
	public static enum Status {
		/**
		 * Initial state.
		 */
		PROTOCOL_INIT,
		/**
		 * Switch to SSL requested, but we still need plaintext mode for the
		 * final step of the negotiation.
		 */
		LIMBO,
		/**
		 * Permanent plaintext mode enabled.
		 */
		PERMANENT
	}

	private static final Logger log = LoggerFactory
			.getLogger(PlainMessageHandler.class);
	private final Object APPOUT_LOCK = new Object();
	private final SelectionKey key;
	private final ByteBuffer appIn, appOut;
	private volatile boolean disposed = false;
	private Status status;

	public PlainMessageHandler(SelectionKey key, int bufsize) {
		if (key == null)
			throw new IllegalArgumentException();
		this.key = key;
		this.appIn = ByteBuffer.allocate(bufsize);
		this.appOut = ByteBuffer.allocate(bufsize);
		status = Status.PROTOCOL_INIT;
	}

	/**
	 * Enqueue a message without flushing the message buffer
	 * 
	 * @param message
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
	 * @throws IOException
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

	@Override
	public void flushMessageQueue() throws IOException {
		flush();
	}

	private final CharsetDecoder decoder = Constants.ENCODING.newDecoder();

	@Override
	public List<NetworkMessage> readRaw() throws IOException, EOFException {
		int bytesRead;
		ReadableByteChannel ch = (ReadableByteChannel) key.channel();
		bytesRead = ch.read(appIn);
		if (bytesRead == -1)
			throw new EOFException();
		if (bytesRead == 0)
			return Collections.emptyList();

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
		// prepare appIn buffer for next read
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
		return result;
	}

	@Override
	public void close() throws IOException {
		key.channel().close();
		key.cancel();
	}

	private void flush() throws IOException {
		if (appOut.position() == 0) {
			log.trace("Nothing to flush");
			synchronized (key) {
				key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
			}
			return;
		}
		SocketChannel ch = (SocketChannel) key.channel();
		log.trace("Will attempt to write {} bytes to {}. Writability {}.",
				appOut.position() - 1, ch.socket().getRemoteSocketAddress(),
				key.isWritable());
		if (!key.isWritable()) {
			synchronized (key) {
				key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
			}
			return;
		}
		appOut.flip();
		ch.write(appOut);
		appOut.compact();
		if (appOut.position() > 0) {
			// short write
			log.trace("Short write! {} bytes left.", appOut.position());
			synchronized (key) {
				key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
			}
		} else {
			log.trace("Write completed successfully.");
			synchronized (key) {
				key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
			}
			if (disposed) {
				close();
			}
		}
	}

	@Override
	public boolean needSend() {
		return appOut.position() > 0;
	}

	public Status getStatus() {
		return status;
	}

	public void setLimbo() {
		if (status != Status.PROTOCOL_INIT)
			throw new IllegalStateException();
		status = Status.LIMBO;
	}

	public void setPermanent() {
		status = Status.PERMANENT;
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
		try {
			send(disconnectMessage);
		} catch (IOException e) {
			log.warn("I/O error during dispose.", e);
		}
		disposed = true;
	}
}
