package be.mapariensis.kanjiryoku.net.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.Kanjiryoku;



public class KanjiServer implements Closeable, Runnable{
	private static final Logger log = LoggerFactory.getLogger(KanjiServer.class);
	private final ServerSocketChannel ssc;
	private final ConnectionMonitor connectionMonitor;
	// listener thread for new connections

	public KanjiServer(int port) throws IOException {
		// get a socket
		ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(port));
		
		// set up the connection monitor
		connectionMonitor = new ConnectionMonitor(ssc);
	}

	@Override
	public void close() throws IOException {
		connectionMonitor.stopListening();
		ssc.close();
	}
	/**
	 * This method doesn't block, it just spawns the monitor thread and exits.
	 */
	@Override
	public void run() {
		log.info("Starting KanjiServer monitor thread...");
		connectionMonitor.start();
	}
	
	public static void main(String[] args) throws IOException {
		Kanjiryoku.loadDLLs();
		KanjiServer s = new KanjiServer(1000);
		s.run();
		while(s.connectionMonitor.isAlive()) {
			try {
				Thread.sleep(100000000000L);
			} catch (InterruptedException e) {
				
			}
		}
	}
}
