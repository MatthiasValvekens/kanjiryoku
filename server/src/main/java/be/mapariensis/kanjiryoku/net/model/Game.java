package be.mapariensis.kanjiryoku.net.model;

import java.io.IOException;
import java.text.ParseException;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
				int seed=(int)(System.currentTimeMillis()%10000);
				log.info("Starting game with seed {}",seed);
				return new TakingTurnsServer(ProblemCollectionUtils.buildKanjiryokuShindanOrganizer(5, new Random(seed)),new ZinniaGuesser("data\\writingmodel\\handwriting-ja.model"));
			} catch (IOException | ParseException e) {
				throw new ServerBackendException(e);
			}
		};
		
		@Override
		public String toString() {
			return "Turn-based Guessing"; 
		}
	};
	private static final Logger log = LoggerFactory.getLogger(Game.class);
	public abstract GameServerInterface getServer() throws UnsupportedGameException, ServerBackendException;
}
