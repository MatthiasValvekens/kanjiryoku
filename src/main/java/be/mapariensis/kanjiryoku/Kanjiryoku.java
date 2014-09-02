package be.mapariensis.kanjiryoku;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.gui.TestPanel;

public class Kanjiryoku {
	private static final Logger log = LoggerFactory.getLogger(Kanjiryoku.class);
	public static void main(String[] args) {
		try {
			System.loadLibrary("jzinnia-0.06-JAVA");
		} catch (UnsatisfiedLinkError err) {
			log.error("Failed to load Zinnia library.",err);
			System.exit(1);
		}
		JFrame frame = new JFrame("Kanji");
		frame.add(new TestPanel());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
	
}
