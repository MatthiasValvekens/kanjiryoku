package be.mapariensis.kanjiryoku.net.secure.auth.db;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.postgresql.ds.PGPoolingDataSource;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.UserData;
import be.mapariensis.kanjiryoku.net.secure.SecurityUtils;
import be.mapariensis.kanjiryoku.net.secure.auth.AuthHandler;
import be.mapariensis.kanjiryoku.net.secure.auth.AuthHandlerProvider;
import be.mapariensis.kanjiryoku.util.IProperties;

public class PostgresAuthProvider implements AuthHandlerProvider {
	public static final String SERVER_NAME = "serverName";
	public static final String DATABASE_NAME = "databaseName";
	public static final String DATABASE_USER = "user";
	public static final String DATABASE_PASSWORD = "password";
	public static final String DATABASE_PORT = "portNumber";

	public static class Factory implements AuthHandlerProvider.Factory {

		@Override
		public AuthHandlerProvider setUp(IProperties config)
				throws BadConfigurationException {
			PGPoolingDataSource ds = new PGPoolingDataSource();

			ds.setServerName(config.getRequired(SERVER_NAME, String.class));
			ds.setPortNumber(config.getRequired(DATABASE_PORT, Integer.class));
			ds.setDatabaseName(config.getRequired(DATABASE_NAME, String.class));
			ds.setUser(config.getRequired(DATABASE_USER, String.class));
			ds.setPassword(config.getRequired(DATABASE_PASSWORD, String.class));

			return new PostgresAuthProvider(ds);
		}

	}

	private final DataSource ds;

	public PostgresAuthProvider(DataSource ds) {
		this.ds = ds;
	}

	@Override
	public AuthHandler createHandler(String username)
			throws UserManagementException, ServerBackendException {
		try {
			return new AuthHandlerImpl(username);
		} catch (SQLException | IllegalArgumentException e) {
			throw new ServerBackendException(e);
		}
	}

	private static final String queryUserInfo = "select id, username, pwhash, salt, created, last_login from kanji_user where username=?";
	private static final String updateLastLogin = "update user set last_login = now() where id=?";

	private class AuthHandlerImpl implements AuthHandler {
		final int id;
		final String salt, pwHash;
		final UserData ud;

		public AuthHandlerImpl(String username) throws SQLException,
				UserManagementException, IllegalArgumentException {
			try (Connection conn = ds.getConnection();
					PreparedStatement ps = conn.prepareStatement(queryUserInfo)) {
				ps.setString(1, username);
				try (ResultSet res = ps.executeQuery()) {
					if (!res.next())
						throw new UserManagementException(
								"User does not exist.");
					this.id = res.getInt("id");
					this.salt = res.getString("salt");
					this.pwHash = res.getString("pwHash");
					if (salt == null || pwHash == null)
						throw new IllegalArgumentException();
					DateTime created = new DateTime(res.getTimestamp("created"));
					DateTime lastLogin = new DateTime(
							res.getTimestamp("last_login"));
					this.ud = new UserData.Builder().setUsername(username)
							.setCreated(created).setLastLogin(lastLogin)
							.deliver();
				}
			}
		}

		@Override
		public String getSalt() {
			return salt;
		}

		@Override
		public UserData getUserData() {
			return ud;
		}

		@Override
		public boolean authenticate(String hash, String clientSalt)
				throws ServerBackendException {
			boolean ok = false;
			// sha bcrypt pwhash from database with client salt
			String finalResult;
			try {
				finalResult = SecurityUtils.sha256(pwHash + clientSalt);
			} catch (NoSuchAlgorithmException e1) {
				throw new ServerBackendException(e1);
			}
			ok = hash.equals(finalResult);
			if (ok) {
				// update last login in database
				try (Connection conn = ds.getConnection();
						PreparedStatement ps = conn
								.prepareStatement(updateLastLogin)) {
					ps.setInt(1, id);
					ps.execute();
				} catch (SQLException e) {
					throw new ServerBackendException(e);
				}
			}
			return ok;
		}

		@Override
		public String getHash() {
			return pwHash;
		}

	}

}
