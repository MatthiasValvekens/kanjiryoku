package be.mapariensis.kanjiryoku.net.model;

import java.io.IOException;
import java.text.ParseException;
import java.util.Random;
import be.mapariensis.kanjiryoku.cr.ZinniaGuesser;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UnsupportedGameException;
import be.mapariensis.kanjiryoku.net.server.GameServerInterface;
import be.mapariensis.kanjiryoku.net.server.games.TakingTurnsServer;
import be.mapariensis.kanjiryoku.util.ProblemCollectionUtils;;
public enum Game {
	//FIXME : move file names to configuration
	TAKINGTURNS {

		@Override
		public GameServerInterface getServer()
				throws UnsupportedGameException, ServerBackendException {
			try {
				return new TakingTurnsServer(ProblemCollectionUtils.buildKanjiryokuShindanOrganizer(5, new Random(3)),new ZinniaGuesser("data\\writingmodel\\handwriting-ja.model"));
			} catch (IOException | ParseException e) {
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
