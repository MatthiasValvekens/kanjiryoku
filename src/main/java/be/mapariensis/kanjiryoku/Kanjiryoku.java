package be.mapariensis.kanjiryoku;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.KanjiGuesser;
import be.mapariensis.kanjiryoku.cr.ZinniaGuesser;
import be.mapariensis.kanjiryoku.gui.GamePanel;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.providers.KanjiryokuShindanParser;

public class Kanjiryoku {
	private static final Logger log = LoggerFactory.getLogger(Kanjiryoku.class);
	public static void main(String[] args) throws IOException, ParseException {
		try {
			System.loadLibrary("jzinnia-0.06-JAVA");
		} catch (UnsatisfiedLinkError err) {
			log.error("Failed to load Zinnia library.",err);
			System.exit(1);
		}
		KanjiGuesser guesser = new ZinniaGuesser("data\\writingmodel\\handwriting-ja.model");
		JFrame frame = new JFrame("Kanji");
		frame.add(new GamePanel(guesser,readLines("data\\problems\\my_kaki_07.txt").iterator()));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
	
	
	private static List<Problem> readLines(String filename) throws IOException {
		ArrayList<Problem> res = new ArrayList<Problem>();
		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename),"Shift-JIS"));
		String line;
		while((line = r.readLine())!=null) {
			try {
				res.add(KanjiryokuShindanParser.parseProblem(line));
			} catch (ParseException e) {}
		}
		return res;
	}
}
