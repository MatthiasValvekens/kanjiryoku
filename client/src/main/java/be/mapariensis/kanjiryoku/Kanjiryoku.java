package be.mapariensis.kanjiryoku;

import java.io.IOException;
import java.net.InetAddress;
import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.gui.InitWindow;
import be.mapariensis.kanjiryoku.gui.MainWindow;

public class Kanjiryoku {
	private static final Logger log = LoggerFactory.getLogger(Kanjiryoku.class);
	private static final boolean SKIP_CONNECT_SCREEN = false; // for testing purposes
	// TODO : allow 
	public static void main(String[] args) {
		if(SKIP_CONNECT_SCREEN) {
			JFrame frame;
			try {
				frame = new MainWindow(InetAddress.getByName("localhost"), 9630, "test"+(System.currentTimeMillis()%10000));
			} catch (IOException e) {
				log.error("I/O error while setting up main window",e);
				return;
			}
			frame.setVisible(true);
		} else {
			InitWindow.show();
		}
		
	}
}
