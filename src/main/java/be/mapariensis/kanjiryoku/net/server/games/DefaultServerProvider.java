package be.mapariensis.kanjiryoku.net.server.games;

import java.io.IOException;
import java.util.Collection;

import be.mapariensis.kanjiryoku.Kanjiryoku;
import be.mapariensis.kanjiryoku.cr.ZinniaGuesser;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UnsupportedGameException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.server.GameServerFactory;
import be.mapariensis.kanjiryoku.net.server.GameServerInterface;

public class DefaultServerProvider implements GameServerFactory {
	private final Collection<Problem> problems;
	public DefaultServerProvider() {
		// read problems FIXME temporary
		try {
			problems = Kanjiryoku.readLines("data\\problems\\my_kaki_07.txt");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public GameServerInterface getServer(Game game)
			throws UnsupportedGameException, ServerBackendException {
		if(game == Game.TAKINGTURNS) {
			try {
				return new TakingTurnsServer(problems.iterator(),new ZinniaGuesser("data\\writingmodel\\handwriting-ja.model"));//FIXME : do this somewhere else, using configuration
			} catch (IOException e) {
				throw new ServerBackendException(e);
			} 
		}
		else throw new UnsupportedGameException(game);
	}

}
