package be.mapariensis.kanjiryoku.net.model;

import org.joda.time.DateTime;

public class UserData {

	public static class Builder {
		private final UserData ud;

		public Builder() {
			ud = new UserData();
		}

		public Builder setUsername(String username) {
			if (username == null)
				throw new IllegalArgumentException();
			ud.username = username;
			return this;
		}

		public Builder setCreated(DateTime created) {
			ud.created = created;
			return this;
		}

		public Builder setLastLogin(DateTime lastLogin) {
			ud.previousLogin = lastLogin;
			return this;
		}

		public Builder setAdmin(boolean isAdmin) {
			ud.isAdmin = isAdmin;
			return this;
		}

		public Builder setId(int id) {
			ud.id = id;
			return this;
		}

		public UserData deliver() {
			if (ud.username == null)
				throw new IllegalStateException();
			return ud;
		}
	}

	/**
	 * User name.
	 */
	private String username;
	/**
	 * Account creation time.
	 */
	private DateTime created;
	/**
	 * Last login.
	 */
	private DateTime previousLogin;

	/**
	 * Admin bit.
	 */
	private boolean isAdmin;

	/**
	 * Internal user ID
	 */
	private int id = -1;

	public String getUsername() {
		return username;
	}

	public DateTime getCreated() {
		return created;
	}

	public DateTime getPreviousLogin() {
		return previousLogin;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public int getId() {
		return id;
	}
}
