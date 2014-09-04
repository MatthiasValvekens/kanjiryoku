package be.mapariensis.kanjiryoku.net.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.util.NetworkThreadFactory;

public class MessageHandler implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
	private final Queue<byte[]> messages = new ConcurrentLinkedQueue<byte[]>(); 
	private final SelectionKey key;
	public MessageHandler(SelectionKey key) {
		if(key==null) throw new IllegalArgumentException();
		this.key = key;
	}
	public void enqueue(String message) {
		enqueue(message.getBytes(Constants.ENCODING));
	}
	public void enqueue(byte[] bytes) {
		messages.add(bytes);
		synchronized(key) {
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
			key.selector().wakeup();
		}
	}
	
	@Override
	public void run() { //only allow one sender per socket at a time
		if(key.isValid() && key.isWritable()) {
			try {
				while(!messages.isEmpty()) {
					ByteBuffer messageBuffer = ((NetworkThreadFactory.NetworkThread)Thread.currentThread()).getBuffer();
					// FFT: check whether key.channel() == channel?
					byte[] msg = messages.poll();
					NetworkMessage.sendRaw((WritableByteChannel)key.channel(),messageBuffer,msg);
				}
			} catch(IOException e) {
				log.error("I/O failure while sending messages",e);
			}
		} else {
			log.error("Channel no longer available for writing.");
		}
	}
		
}
