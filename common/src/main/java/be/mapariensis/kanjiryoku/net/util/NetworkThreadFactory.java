package be.mapariensis.kanjiryoku.net.util;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.concurrent.ThreadFactory;

/**
 * Allows for worker threads with persistent pre-allocated buffers.
 * 
 * @author Matthias Valvekens
 * @version 1.0
 */
public class NetworkThreadFactory implements ThreadFactory {
	private static volatile int factoryCounter = 0;
	private volatile int thisFactoryId;
	private volatile int threadCounter = 0;
	private final int bufsize;
	private final Selector selector;

	public NetworkThreadFactory(int bufsize, Selector selector) {
		this.bufsize = bufsize;
		thisFactoryId = factoryCounter++;
		this.selector = selector;
	}

	public class NetworkThread extends Thread {
		private final ByteBuffer buffer = ByteBuffer.allocate(bufsize);
		private final Runnable job;

		public NetworkThread(Runnable r) {
			super(name(thisFactoryId, threadCounter));
			this.job = r;
			threadCounter++;
		}

		@Override
		public void run() {
			buffer.clear();
			job.run();
			buffer.clear();
			selector.wakeup();
		}

		public ByteBuffer getBuffer() {
			return buffer;
		}
	}

	@Override
	public NetworkThread newThread(Runnable r) {
		return new NetworkThread(r);
	}

	private static String name(int factoryCounter, int threadCounter) {
		return new StringBuilder().append("BufferedThread-")
				.append(factoryCounter).append("-").append(threadCounter)
				.toString();
	}

}
