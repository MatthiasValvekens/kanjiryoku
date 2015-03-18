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
import be.mapariensis.kanjiryoku.net.secure.auth.AuthBackendProvider;
import be.mapariensis.kanjiryoku.net.secure.auth.AuthHandler;
import be.mapariensis.kanjiryoku.util.IProperties;

public class PostgresAuthProvider implements AuthBackendProvider {
	public static final String SERVER_NAME = "serverName";
	public static final String DATABASE_NAME = "databaseName";
	public static final String DATABASE_USER = "user";
	public static final String DATABASE_PASSWORD = "password";
	public static final String DATABASE_PORT = "portNumber";

	public static class Factory implements AuthBackendProvider.Factory {

		@Override
		public AuthBackendProvider setUp(IProperties config)
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

	private static final String queryUserInfo = "select id, username, pwhash, salt, created, last_login, admin from kanji_user where username=?";
	private static final String updateLastLogin = "update kanji_user set last_login = now() where id=?";

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
					boolean isAdmin = res.getBoolean("admin");
					this.ud = new UserData.Builder().setUsername(username)
							.setCreated(created).setLastLogin(lastLogin)
							.setAdmin(isAdmin).deliver();
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

	private static final String UNIQUE_VIOLATED_STATE = "23505";
	private static final String addUser = "insert into kanji_user (username, pwhash, salt) values (?,?,?);";

	@Override
	public void createUser(String username, String hash, String salt)
			throws UserManagementException, ServerBackendException {

		try (Connection conn = ds.getConnection();
				PreparedStatement ps = conn.prepareStatement(addUser)) {
			ps.setString(1, username);
			ps.setString(2, hash);
			ps.setString(3, salt);
			ps.execute();
		} catch (SQLException e) {
			if (UNIQUE_VIOLATED_STATE.equals(e.getSQLState()))
				throw new UserManagementException(String.format(
						"User %s already exists.", username));
			else
				throw new ServerBackendException(e);
		}
	}

	private static final String deleteUser = "delete from kanji_user where username=?";

	@Override
	public void deleteUser(String username) throws ServerBackendException {
		try (Connection conn = ds.getConnection();
				PreparedStatement ps = conn.prepareStatement(deleteUser)) {
			ps.setString(1, username);
			ps.execute();
		} catch (SQLException e) {
			throw new ServerBackendException(e);
		}
	}

	private static final String changePassword = "update kanji_user set pwhash=?, salt=? where username=?";

	@Override
	public void changePassword(String username, String newhash, String newsalt)
			throws UserManagementException, ServerBackendException {
		try (Connection conn = ds.getConnection();
				PreparedStatement ps = conn.prepareStatement(changePassword)) {
			ps.setString(1, newhash);
			ps.setString(2, newsalt);
			ps.setString(3, username);
			int rowsAffected = ps.executeUpdate();
			if (rowsAffected == 0)
				throw new UserManagementException(String.format(
						"User %s does not exist.", username));
		} catch (SQLException e) {
			throw new ServerBackendException(e);
		}
	}

}
