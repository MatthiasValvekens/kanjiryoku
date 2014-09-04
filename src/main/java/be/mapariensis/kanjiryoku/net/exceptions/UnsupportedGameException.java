package be.mapariensis.kanjiryoku.net.exceptions;

import be.mapariensis.kanjiryoku.net.model.Game;

public class UnsupportedGameException extends ServerException {
	public UnsupportedGameException(Game game) {
		this(game.toString());
	}
	public UnsupportedGameException(String name) {
		super(String.format("Unsupported game: %s",name),ERROR_NOT_SUPPORTED);
	}
}
