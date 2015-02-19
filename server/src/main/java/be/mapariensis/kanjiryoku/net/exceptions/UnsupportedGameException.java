package be.mapariensis.kanjiryoku.net.exceptions;

public class UnsupportedGameException extends ServerException {
	public UnsupportedGameException(String name) {
		super(String.format("Unsupported game: %s", name), ERROR_NOT_SUPPORTED);
	}
}
