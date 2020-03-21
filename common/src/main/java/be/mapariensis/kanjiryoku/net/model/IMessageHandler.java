package be.mapariensis.kanjiryoku.net.model;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;

public interface IMessageHandler extends Closeable {

	/**
	 * Enqueue a message without flushing the message buffer
	 */
	void enqueue(NetworkMessage message);

	/**
	 * Buffer a message and flush the buffer while holding the lock on this
	 * handler's application output buffer.
	 */
	void send(NetworkMessage message) throws IOException;

	int flushMessageQueue() throws IOException;

	List<NetworkMessage> readRaw() throws IOException;

	boolean needSend();

	/**
	 * Close the handler after the next flush that completes successfully.
	 */
	void dispose();

	void dispose(String disconnectMessage);

	void dispose(NetworkMessage disconnectMessage);

}