package be.mapariensis.kanjiryoku.net.secure.auth;

import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.model.UserData;

public interface AuthHandler {

    String getSalt() throws ServerBackendException;

    UserData getUserData() throws ServerBackendException;

    boolean authenticate(String hash, String clientSalt)
            throws ServerBackendException;
}
