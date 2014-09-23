package be.mapariensis.kanjiryoku.net.model;

import java.io.IOException;
import java.util.Collection;

import be.mapariensis.kanjiryoku.Kanjiryoku;
import be.mapariensis.kanjiryoku.cr.ZinniaGuesser;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UnsupportedGameException;
import be.mapariensis.kanjiryoku.net.server.GameServerInterface;
import be.mapariensis.kanjiryoku.net.server.games.TakingTurnsServer;

public enum Game {
	TAKINGTURNS {
		private final Collection<Problem> problems;
		{
			try {
				problems = Kanjiryoku.readLines("data\\problems\\my_yomi_07.txt");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public GameServerInterface getServer()
				throws UnsupportedGameException, ServerBackendException {
			try {
				return new TakingTurnsServer(problems.iterator(),new ZinniaGuesser("data\\writingmodel\\handwriting-ja.model"));//FIXME : do this somewhere else, using configuration
			} catch (IOException e) {
				throw new ServerBackendException(e);
			}
		};
		
		@Override
		public String toString() {
			return "Turn-based Guessing"; 
		}
	};
	
	public abstract GameServerInterface getServer() throws UnsupportedGameException, ServerBackendException;
}
