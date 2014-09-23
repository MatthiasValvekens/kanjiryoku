package be.mapariensis.kanjiryoku;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import be.mapariensis.kanjiryoku.gui.MainWindow;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.providers.KanjiryokuShindanParser;
import be.mapariensis.kanjiryoku.providers.ProblemParser;

public class Kanjiryoku {

	public static void main(String[] args) throws IOException, ParseException {
		JFrame frame = new MainWindow(InetAddress.getByName("localhost"), 1000, "test"+(System.currentTimeMillis()%10000));
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	
	public static List<Problem> readLines(String filename) throws IOException {
		ArrayList<Problem> res = new ArrayList<Problem>();
		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename),"Shift-JIS"));
		String line;
		ProblemParser<?> parser = new KanjiryokuShindanParser();
		while((line = r.readLine())!=null) {
			try {
				res.add(parser.parseProblem(line));
			} catch (ParseException e) {}
		}
		return res;
	}
}
