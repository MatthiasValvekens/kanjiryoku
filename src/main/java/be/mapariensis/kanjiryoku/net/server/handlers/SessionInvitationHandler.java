package be.mapariensis.kanjiryoku.net.server.handlers;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.ResponseHandler;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.Session;

public class SessionInvitationHandler implements ResponseHandler {
	private final Session sess;
	public SessionInvitationHandler(Session sess) {
		this.sess = sess;
	}
	private void rejectBroadcast(User user) {
		sess.broadcastHumanMessage(user, String.format("User %s did not accept the invitation.",user.handle));
	}
	@Override
	public void handle(User user, NetworkMessage msg) throws ProtocolSyntaxException, SessionException {
		if(msg.argCount()!=3) {
			rejectBroadcast(user);
			throw new ArgumentCountException(ArgumentCountException.Type.UNEQUAL, Constants.ACCEPTS);
		}
		if(Constants.ACCEPTS.equalsIgnoreCase(msg.get(1)) && String.valueOf(sess.getId()).equals(msg.get(2))) {
			sess.addMember(user);
		} else {
			rejectBroadcast(user);
		}
	}
}
