package be.mapariensis.kanjiryoku.net.server;

import be.mapariensis.kanjiryoku.net.model.IMessageHandler;
import be.mapariensis.kanjiryoku.net.secure.auth.ServerAuthEngine;

public class ConnectionContext {
    private IMessageHandler messageHandler;
    private ServerAuthEngine authEngine;

    public IMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(IMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public ServerAuthEngine getAuthEngine() {
        return authEngine;
    }

    public void setAuthEngine(ServerAuthEngine authEngine) {
        this.authEngine = authEngine;
    }
}
