package be.mapariensis.kanjiryoku.net.model;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;

public class MessageHandler implements Runnable, Closeable {
	private static final Logger log = LoggerFactory
			.getLogger(MessageHandler.class);
	private final Queue<byte[]> messages = new ConcurrentLinkedQueue<byte[]>();
	private volatile boolean sendingNow = false;
	private final SelectionKey key;
	private static final byte[] GOODBYE = ServerCommandList.BYE.toString()
			.getBytes();
	private final ByteBuffer netIn, netOut;

	public MessageHandler(SelectionKey key, int bufsize) {
		if (key == null)
			throw new IllegalArgumentException();
		this.key = key;
		this.netIn = ByteBuffer.allocate(bufsize);
		this.netOut = ByteBuffer.allocate(bufsize);
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
						sendMessageNow(msg, netOut);
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

	private void sendMessageNow(byte[] msg, ByteBuffer messageBuffer)
			throws IOException {
		NetworkMessage.sendRaw((WritableByteChannel) key.channel(),
				messageBuffer, msg);
	}

	@Override
	public void close() throws IOException {
		synchronized (key) {
			sendMessageNow(GOODBYE, ByteBuffer.allocate(GOODBYE.length + 1));
		}
	}
}
