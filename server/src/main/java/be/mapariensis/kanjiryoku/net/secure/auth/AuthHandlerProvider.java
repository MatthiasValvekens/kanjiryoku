package be.mapariensis.kanjiryoku.net.secure.auth;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface AuthHandlerProvider {
	public interface Factory {
		public AuthHandlerProvider setUp(IProperties config)
				throws BadConfigurationException;
	}

	public AuthHandler createHandler(String username)
			throws UserManagementException, ServerBackendException;
}
