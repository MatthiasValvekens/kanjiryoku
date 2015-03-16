package be.mapariensis.kanjiryoku.gui.macros;

import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;

public class MacroException extends ClientException {

	public MacroException(String message) {
		super(message, ClientServerException.ERROR_UI);
	}

	public MacroException(String message, Throwable cause) {
		super(message, cause, ClientServerException.ERROR_UI);
	}

	public MacroException(Throwable cause) {
		super(cause, ClientServerException.ERROR_UI);
	}

}
