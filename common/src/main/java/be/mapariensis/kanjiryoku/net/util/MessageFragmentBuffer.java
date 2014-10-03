package be.mapariensis.kanjiryoku.net.util;

import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class MessageFragmentBuffer {
	private static final Logger log = LoggerFactory.getLogger(MessageFragmentBuffer.class);
	private final ByteBuffer buf;
	private volatile boolean readingPartialMessage = false;
	
	public MessageFragmentBuffer(int size) {
		buf = ByteBuffer.allocate(size);
	}
	
	public synchronized NetworkMessage postMessage(byte[] message, int offset, int length) {
		buf.put(message,offset,length);
		if(message[offset+length-1] == NetworkMessage.EOM) {
			log.info("Finalizing partial message");
			byte[] fullMessage = new byte[buf.position()-1]; // skip EOM
			((ByteBuffer)buf.flip()).get(fullMessage); // read from pos zero to current position
			String messageString = new String(fullMessage,Constants.ENCODING);
			readingPartialMessage=false;
			buf.clear();
			// assume only one message is in the buffer at one time (this is up to the caller to enforce)
			return NetworkMessage.buildArgs(messageString);
		} else {
			if(!readingPartialMessage) {
				log.info("Received partial message.");
				readingPartialMessage = true;
			}
			return null;
		}
	}
	
	public boolean readingPartialMessage() {
		return readingPartialMessage;
	}
	
}
