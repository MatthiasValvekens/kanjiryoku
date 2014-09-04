package be.mapariensis.kanjiryoku.net.server.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.ResponseHandler;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.Session;

public class SessionInvitationHandler implements ResponseHandler {
	private static final Logger log = LoggerFactory.getLogger(SessionInvitationHandler.class);
	private final Session sess;
	public SessionInvitationHandler(Session sess) {
		this.sess = sess;
	}
	@Override
	public void handle(User user, NetworkMessage msg) throws ProtocolSyntaxException, SessionException {
		if(msg.argCount()!=3) throw new ProtocolSyntaxException("Session invitation response takes three arguments. Rejected");
		if("OK".equalsIgnoreCase(msg.get(1)) && String.valueOf(sess.getId()).equals(msg.get(2))) {
			if(sess.isDestroyed()) {
				log.warn("Session {} is already destroyed.",sess);
				return;
			}
			
			sess.addMember(user);
		} else {
			sess.broadcastHumanMessage(user, String.format("User %s did not accept the invitation.",user.handle));
		}
	}
}
