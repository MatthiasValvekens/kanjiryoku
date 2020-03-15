package be.mapariensis.kanjiryoku.net.secure.auth;

import be.mapariensis.kanjiryoku.config.ServerConfig;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface AuthBackendProvider {
	interface Factory {
		AuthBackendProvider setUp(ServerConfig serverConfig,
				IProperties authConfig) throws BadConfigurationException;
	}

	AuthHandler createHandler(String username)
			throws UserManagementException, ServerBackendException;

	void createUser(String username, String hash, String salt)
			throws UserManagementException, ServerBackendException;

	void deleteUser(String username) throws ServerBackendException;

	void changePassword(String username, String newhash, String newsalt)
			throws UserManagementException, ServerBackendException;
}
