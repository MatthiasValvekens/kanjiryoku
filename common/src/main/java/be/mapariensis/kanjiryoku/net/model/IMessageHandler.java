package be.mapariensis.kanjiryoku.net.model;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import javax.net.ssl.SSLEngine;

public interface IMessageHandler extends Closeable {

	/**
	 * Enqueue a message without flushing the message buffer
	 * 
	 * @param message
	 */
	public abstract void enqueue(NetworkMessage message);

	/**
	 * Buffer a message and flush the buffer while holding the lock on this
	 * handler's application output buffer.
	 * 
	 * @param message
	 */
	public abstract void send(NetworkMessage message);

	public abstract SSLEngine getSSLEngine();

	public abstract void flushMessageQueue();

	public abstract List<NetworkMessage> readRaw() throws IOException,
			EOFException;

	public abstract boolean needSend();

}