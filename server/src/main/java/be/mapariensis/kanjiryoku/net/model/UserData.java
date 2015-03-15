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
			if (created == null)
				throw new IllegalArgumentException();
			ud.created = created;
			return this;
		}

		public Builder setLastLogin(DateTime lastLogin) {
			if (lastLogin == null)
				throw new IllegalArgumentException();
			ud.previousLogin = lastLogin;
			return this;
		}

		public UserData deliver() {
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

	public String getUsername() {
		return username;
	}

	public DateTime getCreated() {
		return created;
	}

	public DateTime getPreviousLogin() {
		return previousLogin;
	}

}
