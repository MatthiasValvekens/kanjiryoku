package be.mapariensis.kanjiryoku.net.model;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
	private final Object readLock = new Object();
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
		netOut.clear();
		netOut.put(message);
		netOut.put(NetworkMessage.EOM);
		netOut.flip();
		WritableByteChannel ch = (WritableByteChannel) key.channel();
		synchronized (key) {
			ch.write(netOut);
		}
	}

	private final CharsetDecoder decoder = Constants.ENCODING.newDecoder();

	public List<NetworkMessage> readRaw() throws IOException, EOFException {
		synchronized (readLock) {
			int bytesRead;
			ReadableByteChannel ch = (ReadableByteChannel) key.channel();
			synchronized (key) {
				bytesRead = ch.read(netIn);
			}
			if (bytesRead == -1)
				throw new EOFException();
			if (bytesRead == 0)
				return Arrays.asList(new NetworkMessage());

			// need to remember this for later
			int finalPosition = netIn.position();
			netIn.limit(finalPosition);
			// search backwards until we find the first EOM
			int lastEom;
			for (lastEom = netIn.limit() - 1; lastEom >= 0; lastEom--) {
				netIn.position(lastEom);
				if (netIn.get() == NetworkMessage.EOM)
					break;
			}
			// no full message received
			// the position is now at 0
			if (lastEom == -1) {
				netIn.compact();
				return Collections.emptyList();
			}
			// the position is one byte after the last EOM
			// so we can flip, and compact in the end
			netIn.flip();

			// decode the input into network messages
			CharBuffer decodedInput = CharBuffer.allocate(bytesRead);
			// there should not be any incomplete characters,
			// after all, we cut off at the last EOM
			decoder.decode(netIn, decodedInput, true);
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
			netIn.limit(finalPosition);
			netIn.compact();

			return result;
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (key) {
			sendMessageNow(GOODBYE);
		}
	}
}
