package be.mapariensis.kanjiryoku.net.secure.auth;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface AuthBackendProvider {
	public interface Factory {
		public AuthBackendProvider setUp(IProperties config)
				throws BadConfigurationException;
	}

	public AuthHandler createHandler(String username)
			throws UserManagementException, ServerBackendException;

	public void createUser(String username, String hash, String salt)
			throws UserManagementException, ServerBackendException;

	public void deleteUser(String username) throws ServerBackendException;
}
