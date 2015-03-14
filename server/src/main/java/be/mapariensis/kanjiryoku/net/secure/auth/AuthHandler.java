package be.mapariensis.kanjiryoku.net.secure.auth;

import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.model.UserData;

public interface AuthHandler {

	public String getSalt() throws ServerBackendException;

	public UserData getUserData() throws ServerBackendException;

	public boolean authenticate(String hash, String clientSalt)
			throws ServerBackendException;

	public String getHash() throws ServerBackendException;
}
