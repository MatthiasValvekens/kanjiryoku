package be.mapariensis.kanjiryoku.net.secure.auth;

public enum AuthStatus {
	/**
	 * The initial state, nothing has happened yet.
	 */
	INIT,
	/**
	 * The server is waiting for credentials.
	 */
	WAIT_CRED,
	/**
	 * The server is processing the credentials.
	 */
	PROCESS_CRED,
	/**
	 * Authentication succeeded.
	 */
	SUCCESS,
	/**
	 * Authentication failed.
	 */
	FAILURE
}