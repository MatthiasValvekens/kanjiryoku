package be.mapariensis.kanjiryoku.net.model;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.ArrayList;

import be.mapariensis.kanjiryoku.cr.ZinniaGuesser;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UnsupportedGameException;
import be.mapariensis.kanjiryoku.net.server.GameServerInterface;
import be.mapariensis.kanjiryoku.net.server.games.TakingTurnsServer;
import be.mapariensis.kanjiryoku.providers.KanjiryokuShindanParser;
import be.mapariensis.kanjiryoku.providers.ProblemParser;
import be.mapariensis.kanjiryoku.util.ProblemCollectionUtils;;
public enum Game {
	//FIXME : move file names to configuration
	TAKINGTURNS {
		private final Collection<Problem> problems;
		{
			ProblemParser<?> p = new KanjiryokuShindanParser();
			try {
				Collection<String> names = new LinkedList<String>();
				for(int i = 1;i<=11;i++){
					names.add(String.format(format,"yomi",i));
					names.add(String.format(format,"kaki",i));
				}
				Collection<String> definitions = ProblemCollectionUtils.collectFromFiles(names,5,TOTAL_PROBLEMS,Charset.forName("Shift-JIS"),new Random(3));
				problems = new ArrayList<Problem>(definitions.size());
				for(String def : definitions) problems.add(p.parseProblem(def));
			} catch (IOException | ParseException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public GameServerInterface getServer()
				throws UnsupportedGameException, ServerBackendException {
			try {
				return new TakingTurnsServer(problems.iterator(),new ZinniaGuesser("data\\writingmodel\\handwriting-ja.model"));
			} catch (IOException e) {
				throw new ServerBackendException(e);
			}
		};
		
		@Override
		public String toString() {
			return "Turn-based Guessing"; 
		}
	};
	private static final String format = "data\\problems\\my_%s_%02d.txt";
	private static final int TOTAL_PROBLEMS = 5;
	public abstract GameServerInterface getServer() throws UnsupportedGameException, ServerBackendException;
}
