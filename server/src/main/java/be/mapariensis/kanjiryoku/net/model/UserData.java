package be.mapariensis.kanjiryoku.net.model;

import org.joda.time.DateTime;

public class UserData {
	/**
	 * User name.
	 */
	public final String username;
	/**
	 * Account creation time.
	 */
	public final DateTime created;
	/**
	 * Last login.
	 */
	public final DateTime previousLogin;

	public UserData(String username, DateTime created, DateTime lastLogin) {
		if (username == null || created == null || lastLogin == null)
			throw new IllegalArgumentException();
		this.username = username;
		this.created = created;
		this.previousLogin = lastLogin;
	}
}
