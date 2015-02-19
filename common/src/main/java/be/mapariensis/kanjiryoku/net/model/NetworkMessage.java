package be.mapariensis.kanjiryoku.net.model;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static be.mapariensis.kanjiryoku.net.Constants.*;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.util.MessageFragmentBuffer;

public class NetworkMessage implements Iterable<String>,
		Comparable<NetworkMessage> {
	private static final Logger log = LoggerFactory
			.getLogger(NetworkMessage.class);
	public static final char ATOMIZER = '"';
	public static final char DELIMITER = ' ';
	public static final char ESCAPE_CHAR = '\\';
	public static final byte EOM = (byte) 0x01;
	private static final String EOMSTR = new String(new byte[] { EOM });

	private final List<?> args;
	public final long timestamp;

	public NetworkMessage(Object obj, List<String> args) {
		ArrayList<Object> objs = new ArrayList<Object>();
		objs.add(obj);
		objs.addAll(args);
		this.args = objs;
		timestamp = System.currentTimeMillis();
	}

	public NetworkMessage(Object... args) {
		this(Arrays.asList(args));
	}

	public NetworkMessage(List<?> args) {
		this.args = args;
		timestamp = System.currentTimeMillis();
	}

	public String get(int ix) {
		return String.valueOf(args.get(ix));
	}

	public NetworkMessage truncate(int start) {
		return truncate(start, args.size());
	}

	public NetworkMessage truncate(int start, int end) {
		return new NetworkMessage(args.subList(start, end));
	}

	public static List<NetworkMessage> readRaw(ReadableByteChannel sock,
			ByteBuffer buf, MessageFragmentBuffer mfb) throws IOException,
			EOFException {
		buf.clear();
		int bytesRead;
		synchronized (sock) {
			bytesRead = sock.read(buf);
		}
		if (bytesRead == -1)
			throw new EOFException();
		if (bytesRead == 0)
			return Arrays.asList(new NetworkMessage());
		byte[] msgBytes = new byte[bytesRead];
		((ByteBuffer) buf.flip()).get(msgBytes); // read from pos zero to
													// current position
		String msgString = new String(msgBytes, ENCODING);
		String[] lines = msgString.split(EOMSTR);
		ArrayList<NetworkMessage> result = new ArrayList<NetworkMessage>(
				lines.length);
		if (msgBytes[msgBytes.length - 1] != EOM) {
			int i;
			// find last EOM in message
			for (i = msgBytes.length - 1; i > 0; i--) {
				if (msgBytes[i - 1] == EOM)
					break;
			}
			// partial message
			mfb.postMessage(msgBytes, i, msgBytes.length - i);
			lines[lines.length - 1] = null; // mark last part as invalid
		} else if (mfb.readingPartialMessage()) { // submitting final part of
													// message.
			// find first EOM in message
			int i;
			for (i = 0; i < msgBytes.length; i++) {
				if (msgBytes[i] == EOM)
					break;
			}
			NetworkMessage msg = mfb.postMessage(msgBytes, 0, i + 1); // ensure
																		// the
																		// EOM
																		// is
																		// included
			if (msg == null) {
				log.error("Expected finalized message, but got null");
			} else {
				result.add(msg);
			}
			lines[0] = null; // mark first part as invalid
		}

		for (String line : lines) {
			if (line == null)
				continue;
			result.add(buildArgs(line));
		}
		return result;
	}

	public void sendRaw(WritableByteChannel sock, ByteBuffer buf)
			throws IOException {
		buf.clear();
		String message = toString();
		if (message.contains(EOMSTR))
			throw new IOException("EOM byte is illegal in messages.");
		buf.put(message.getBytes());
		buf.put(EOM);
		buf.flip();
		synchronized (sock) {
			sock.write(buf);
		}
	}

	@Override
	public String toString() {
		return toString(0, args.size());
	}

	public String toString(int beginIndex) {
		return toString(beginIndex, args.size());
	}

	public String toString(int beginIndex, int endIndex) {
		if (argCount() == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		int i;
		for (i = beginIndex; i < endIndex - 1; i++) {
			sb.append(escapedAtom(get(i)));
			sb.append(String.valueOf(DELIMITER));
		}
		sb.append(escapedAtom(get(i)));
		return sb.toString();
	}

	public static void sendRaw(WritableByteChannel sock, ByteBuffer buf,
			String message) throws IOException {
		if (message.contains(EOMSTR))
			throw new IOException("EOM byte is illegal in messages.");
		sendRaw(sock, buf, message.getBytes(ENCODING));
	}

	public static void sendRaw(WritableByteChannel sock, ByteBuffer buf,
			byte[] message) throws IOException {
		for (byte b : message) {
			if (b == EOM)
				throw new IOException("EOM byte is illegal in messages.");
		}
		buf.clear();
		buf.put(message);
		buf.put(EOM);
		buf.flip();
		synchronized (sock) {
			sock.write(buf);
		}
	}

	public static void signalProcessingError(WritableByteChannel sock,
			ClientServerException ex) throws IOException {
		signalProcessingError(sock,
				ByteBuffer.allocate(ex.getMessage().length()), ex);
	}

	public static void signalProcessingError(WritableByteChannel sock,
			ByteBuffer buf, ClientServerException ex) throws IOException {
		sendRaw(sock, buf, ex.getMessage()); // these exception messages are
												// protocol compliant
	}

	// TODO unit tests
	public static NetworkMessage buildArgs(String in) {
		in = in.trim();
		List<String> result = new ArrayList<String>();
		boolean ignoreDelims = false, escape = false;
		StringBuilder sb = new StringBuilder(in.length());
		for (int i = 0; i < in.length(); i++) {
			char cur = in.charAt(i);
			if (cur == ESCAPE_CHAR) {
				if (escape)
					sb.append(ESCAPE_CHAR);
				else
					escape = true;
			} else {
				if (cur == ATOMIZER && !escape)
					ignoreDelims = !ignoreDelims;
				else if (cur == DELIMITER && !ignoreDelims && sb.length() != 0
						&& !escape) { // check for delims
					result.add(sb.toString());
					sb.setLength(0); // clear buffer
				} else
					sb.append(cur);
				escape = false;
			}
		}
		String last = sb.toString();
		if (!last.isEmpty())
			result.add(last.trim());
		return new NetworkMessage(result);
	}

	@Override
	public Iterator<String> iterator() {
		return new Iterator<String>() {
			Iterator<?> backend = args.iterator();

			@Override
			public boolean hasNext() {
				return backend.hasNext();
			}

			@Override
			public String next() {
				return String.valueOf(backend.next());
			}

			@Override
			public void remove() {
				backend.remove();
			}

		};
	}

	public int argCount() {
		return args.size();
	}

	public boolean isEmpty() {
		return args.isEmpty();
	}

	private static final String escapedAtomizer = new StringBuilder()
			.append(ESCAPE_CHAR).append(ATOMIZER).toString();

	public static String escapeSpecial(String string) { // does not escape
														// delimiters
		return string.replace(String.valueOf(ESCAPE_CHAR), escapedEscape)
				.replace(String.valueOf(ATOMIZER), escapedAtomizer);
	}

	private static final String escapedEscape = new StringBuilder()
			.append(ESCAPE_CHAR).append(ESCAPE_CHAR).toString();

	private static String atomize(String string) {
		if (string.indexOf(DELIMITER) != -1) {
			return new StringBuilder().append(ATOMIZER).append(string)
					.append(ATOMIZER).toString();
		} else
			return string;
	}

	public static String escapedAtom(String string) {
		return atomize(escapeSpecial(string));
	}

	public NetworkMessage concatenate(Object... args) {
		return concatenate(Arrays.asList(args));
	}

	public NetworkMessage concatenate(List<?> args) {
		ArrayList<Object> newargs = new ArrayList<Object>(this.args);
		newargs.addAll(args);
		return new NetworkMessage(newargs);
	}

	@Override
	public int compareTo(NetworkMessage o) {
		if (o.timestamp == timestamp)
			return toString().compareTo(o.toString());
		long diff = timestamp - o.timestamp;
		return diff < 0 ? -1 : 1;
	}

}
