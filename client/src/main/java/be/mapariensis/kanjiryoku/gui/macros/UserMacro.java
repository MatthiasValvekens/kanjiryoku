package be.mapariensis.kanjiryoku.gui.macros;

import be.mapariensis.kanjiryoku.gui.GUIBridge;
import be.mapariensis.kanjiryoku.net.client.handlers.SignupProtocolHandler;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public enum UserMacro {
	SIGNUP {

		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws MacroException {
			checkArgs(msg, 3);
			String username = msg.get(1);
			String password = msg.get(2);
			new SignupProtocolHandler(bridge.getUplink(), username, password)
					.requestSignup();
		}

	};
	public abstract void execute(NetworkMessage msg, GUIBridge bridge)
			throws MacroException;

	public static void checkArgs(NetworkMessage msg, int args)
			throws MacroException {
		if (msg.argCount() != args)
			throw new MacroException(String.format(
					"This macro takes %d arguments", args - 1));
	}
}
