package be.mapariensis.kanjiryoku.net.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class KanjiServer implements Closeable {
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
	
	public static void main(String[] args) throws IOException {
		new KanjiServer(1000).connectionMonitor.run();
	}
}
