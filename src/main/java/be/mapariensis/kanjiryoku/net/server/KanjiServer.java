package be.mapariensis.kanjiryoku.net.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.Kanjiryoku;



public class KanjiServer {
	private static final Logger log = LoggerFactory.getLogger(Kanjiryoku.class);
	public static void loadDLLs() {
		try {
			System.loadLibrary("jzinnia-0.06-JAVA");
		} catch (UnsatisfiedLinkError err) {
			log.error("Failed to load Zinnia library.",err);
			System.exit(1);
		}
	}
	public static void main(String[] args) throws IOException {
		loadDLLs();
		try(ConnectionMonitor s = new ConnectionMonitor(1000)) {
			s.start();
			while(s.isAlive()) {
				try {
					Thread.sleep(100000000000L);
				} catch (InterruptedException e) {
					
				}
			}
		}
	}
}
