package be.mapariensis.kanjiryoku.net.exceptions;

public class GameFlowException extends ServerException {

	public GameFlowException(String message) {
		super(message, ERROR_GAME_INTERNAL);
	}

	public GameFlowException(String message, Throwable cause) {
		super(message, cause, ERROR_GAME_INTERNAL);
	}

	public GameFlowException(Throwable cause) {
		super(cause, ERROR_GAME_INTERNAL);
	}


}
