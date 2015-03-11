package be.mapariensis.kanjiryoku.net.secure.auth;

import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.UserData;

public interface AuthHandler {

	public interface Factory {
		public AuthHandler init(String username) throws UserManagementException;
	}

	public String getSalt();

	public UserData getUserData();

	public boolean authenticate(String hash, String clientSalt);

	public String getHash();
}
