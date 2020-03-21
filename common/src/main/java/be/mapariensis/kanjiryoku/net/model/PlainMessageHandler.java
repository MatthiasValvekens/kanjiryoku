package be.mapariensis.kanjiryoku.net.model;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.commands.ClientCommandList;

public class PlainMessageHandler extends MessageHandler {
	public enum Status {
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
	private Status status;

	public PlainMessageHandler(SelectionKey key, int bufsize) {
		super(key);
		this.key = key;
		this.appIn = ByteBuffer.allocate(bufsize);
		this.appOut = ByteBuffer.allocate(bufsize);
		status = Status.PROTOCOL_INIT;
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

	@Override
	public int flushMessageQueue() throws IOException {
		return flush();
	}

	@Override
	protected ByteBuffer getNetworkOutputBuffer() {
		return appOut;
	}

	@Override
	protected ByteBuffer getApplicationInputBuffer() {
		return appIn;
	}

	@Override
	public List<NetworkMessage> readRaw() throws IOException {
		int bytesRead;
		ReadableByteChannel ch = (ReadableByteChannel) key.channel();
		bytesRead = ch.read(appIn);
		if (bytesRead == -1)
			throw new EOFException();
		if (bytesRead == 0)
			return Collections.emptyList();

		return super.readRaw();
	}

	@Override
	public void close() throws IOException {
		key.channel().close();
		key.cancel();
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
		disposed = true;
		try {
			send(disconnectMessage);
		} catch (IOException e) {
			log.warn("I/O error during dispose.", e);
		}
	}
}
