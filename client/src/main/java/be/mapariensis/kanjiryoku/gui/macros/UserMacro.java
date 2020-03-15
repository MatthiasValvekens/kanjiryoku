package be.mapariensis.kanjiryoku.gui.macros;

import be.mapariensis.kanjiryoku.gui.GUIBridge;
import be.mapariensis.kanjiryoku.net.client.handlers.SignupProtocolHandler;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

@SuppressWarnings("unused")
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

	},
	RESETPASS {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws MacroException {
			int argcount = msg.argCount();
			if (argcount < 2 || argcount > 3)
				throw new MacroException("This macro takes 1 or 2 arguments");
			String username, password;

			// default to changing the user's own password
			if (argcount == 2) {
				username = bridge.getUplink().getUsername();
				password = msg.get(1);
			} else {
				username = msg.get(1);
				password = msg.get(2);
			}

			new SignupProtocolHandler(bridge.getUplink(), username, password)
					.requestPasswordChange();
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
