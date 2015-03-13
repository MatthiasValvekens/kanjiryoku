package be.mapariensis.kanjiryoku.net.secure.auth;

import be.mapariensis.kanjiryoku.net.model.UserData;

public interface AuthHandler {

	public String getSalt();

	public UserData getUserData();

	public boolean authenticate(String hash, String clientSalt);

	public String getHash();
}
