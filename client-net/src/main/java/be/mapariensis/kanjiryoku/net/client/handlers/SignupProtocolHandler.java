package be.mapariensis.kanjiryoku.net.client.handlers;

import be.mapariensis.kanjiryoku.net.exceptions.ServerSubmissionException;
import org.mindrot.jbcrypt.BCrypt;

import be.mapariensis.kanjiryoku.net.client.ServerResponseHandler;
import be.mapariensis.kanjiryoku.net.client.ServerUplink;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class SignupProtocolHandler extends ServerResponseHandler {
    private final String username, password;
    private final ServerUplink uplink;

    public SignupProtocolHandler(ServerUplink uplink, String username,
            String password) {
        this.username = username;
        this.password = password;
        this.uplink = uplink;
    }

    public void requestSignup() throws ServerSubmissionException {
        NetworkMessage msg = new NetworkMessage("ADMIN", "ADDUSER", id,
                username);
        uplink.enqueueMessage(msg, this);
    }

    public void requestPasswordChange() throws ServerSubmissionException {
        NetworkMessage msg = new NetworkMessage("RESETPASS", id, username);
        uplink.enqueueMessage(msg, this);
    }

    @Override
    public void handle(NetworkMessage msg) throws ClientException {
        if (msg.argCount() != 4)
            throw new ServerCommunicationException(msg);

        int responseCode;
        try {
            responseCode = Integer.parseInt(msg.get(2));
        } catch (RuntimeException ex) {
            throw new ServerCommunicationException(ex);
        }
        String salt = msg.get(3);
        String dbHash = BCrypt.hashpw(password, salt);
        NetworkMessage reply = new NetworkMessage(ServerCommandList.RESPOND,
                responseCode, dbHash);
        uplink.enqueueMessage(reply);
    }

}
