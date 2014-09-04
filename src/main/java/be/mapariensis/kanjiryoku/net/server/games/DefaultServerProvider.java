package be.mapariensis.kanjiryoku.net.server.games;

import java.io.IOException;

import be.mapariensis.kanjiryoku.cr.ZinniaGuesser;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UnsupportedGameException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.server.GameServerFactory;
import be.mapariensis.kanjiryoku.net.server.GameServerInterface;

public class DefaultServerProvider implements GameServerFactory {

	@Override
	public GameServerInterface getServer(Game game)
			throws UnsupportedGameException, ServerBackendException {
		if(game == Game.TAKINGTURNS) {
			try {
				return new TakingTurnsServer(new ZinniaGuesser("data\\writingmodel\\handwriting-ja.model"));//FIXME : do this somewhere else, using configuration
			} catch (IOException e) {
				throw new ServerBackendException(e);
			} 
		}
		else throw new UnsupportedGameException(game);
	}

}
