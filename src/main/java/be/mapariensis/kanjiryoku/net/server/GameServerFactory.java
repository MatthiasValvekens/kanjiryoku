package be.mapariensis.kanjiryoku.net.server;

import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UnsupportedGameException;
import be.mapariensis.kanjiryoku.net.model.Game;

public interface GameServerFactory {
	public GameServerInterface getServer(Game game) throws UnsupportedGameException, ServerBackendException;
}
