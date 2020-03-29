package be.mapariensis.kanjiryoku.net.server.handlers;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.ClientResponseHandler;
import be.mapariensis.kanjiryoku.net.server.Session;

public class SessionInvitationHandler extends ClientResponseHandler {
    private final Session sess;

    public SessionInvitationHandler(Session sess2) {
        this.sess = sess2;
    }

    private void rejectBroadcast(User user) {
        sess.broadcastHumanMessage(user, String.format(
                "User %s did not accept the invitation.", user.handle));
    }

    @Override
    public void handle(User user, NetworkMessage msg)
            throws ProtocolSyntaxException, SessionException {
        if (msg.argCount() == 3 && msg.get(2).equals(Constants.REJECTS)) {
            rejectBroadcast(user);
        }
        if (msg.argCount() != 4) {
            rejectBroadcast(user);
            throw new ArgumentCountException(
                    ArgumentCountException.Type.UNEQUAL, Constants.ACCEPTS);
        }
        if (Constants.ACCEPTS.equalsIgnoreCase(msg.get(2))
                && String.valueOf(sess.getId()).equals(msg.get(3))) {
            sess.addMember(user);
        } else {
            rejectBroadcast(user);
        }
    }
}
