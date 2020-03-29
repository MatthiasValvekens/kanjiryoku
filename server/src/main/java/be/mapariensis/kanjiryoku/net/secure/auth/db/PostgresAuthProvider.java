package be.mapariensis.kanjiryoku.net.secure.auth.db;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.joda.time.DateTime;

import be.mapariensis.kanjiryoku.config.ServerConfig;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.UserData;
import be.mapariensis.kanjiryoku.net.secure.SecurityUtils;
import be.mapariensis.kanjiryoku.net.secure.auth.AuthBackendProvider;
import be.mapariensis.kanjiryoku.net.secure.auth.AuthHandler;
import be.mapariensis.kanjiryoku.persistent.PostgresProvider;
import be.mapariensis.kanjiryoku.persistent.util.NamedPreparedStatement;
import be.mapariensis.kanjiryoku.persistent.util.StatementIndexer;
import be.mapariensis.kanjiryoku.util.IProperties;

public class PostgresAuthProvider implements AuthBackendProvider {

    public static class Factory implements AuthBackendProvider.Factory {

        @Override
        public AuthBackendProvider setUp(ServerConfig serverConfig,
                IProperties authConfig) throws BadConfigurationException {
            DataSource ds = serverConfig.getDbConnection();
            if (ds == null) {
                // try postgresprovider

                if ((ds = (new PostgresProvider()).getDataSource(authConfig)) == null) {
                    throw new BadConfigurationException(
                            "No data source available");
                }
            }
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

    private static final String queryUserInfoSql = "select id, username, pwhash, salt, created, last_login, admin from kanji_user where username=${username}";
    private static final StatementIndexer queryUserInfo = new StatementIndexer(
            queryUserInfoSql);
    private static final String updateLastLoginSql = "update kanji_user set last_login = now() where id=${id}";
    private static final StatementIndexer updateLastLogin = new StatementIndexer(
            updateLastLoginSql);

    private class AuthHandlerImpl implements AuthHandler {
        final int id;
        final String salt, pwHash;
        final UserData ud;

        public AuthHandlerImpl(String username) throws SQLException,
                UserManagementException, IllegalArgumentException {
            try (Connection conn = ds.getConnection();
                    NamedPreparedStatement ps = queryUserInfo
                            .prepareStatement(conn)) {
                ps.setString("username", username);
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
                            .setAdmin(isAdmin).setId(id).deliver();
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
            // sha bcrypt pwhash from database with client salt
            String finalResult;
            try {
                finalResult = SecurityUtils.sha256(pwHash + clientSalt);
            } catch (NoSuchAlgorithmException e1) {
                throw new ServerBackendException(e1);
            }
            boolean ok = hash.equals(finalResult);
            if (ok) {
                // update last login in database
                try (Connection conn = ds.getConnection();
                        NamedPreparedStatement ps = updateLastLogin
                                .prepareStatement(conn)) {
                    ps.setInt("id", id);
                    ps.execute();
                } catch (SQLException e) {
                    throw new ServerBackendException(e);
                }
            }
            return ok;
        }
    }

    private static final String UNIQUE_VIOLATED_STATE = "23505";
    private static final String addUserSql = "insert into kanji_user (username, pwhash, salt) values (${username},${pwhash},${salt});";
    private static final StatementIndexer addUser = new StatementIndexer(
            addUserSql);

    @Override
    public void createUser(String username, String hash, String salt)
            throws UserManagementException, ServerBackendException {

        try (Connection conn = ds.getConnection();
                NamedPreparedStatement ps = addUser.prepareStatement(conn)) {
            ps.setString("username", username);
            ps.setString("pwhash", hash);
            ps.setString("salt", salt);
            ps.execute();
        } catch (SQLException e) {
            if (UNIQUE_VIOLATED_STATE.equals(e.getSQLState()))
                throw new UserManagementException(String.format(
                        "User %s already exists.", username));
            else
                throw new ServerBackendException(e);
        }
    }

    private static final String deleteUserSql = "delete from kanji_user where username=${username}";
    private static final StatementIndexer deleteUser = new StatementIndexer(
            deleteUserSql);

    @Override
    public void deleteUser(String username) throws ServerBackendException {
        try (Connection conn = ds.getConnection();
                NamedPreparedStatement ps = deleteUser.prepareStatement(conn)) {
            ps.setString("username", username);
            ps.execute();
        } catch (SQLException e) {
            throw new ServerBackendException(e);
        }
    }

    private static final String changePasswordSql = "update kanji_user set pwhash=${pwhash}, salt=${salt} where username=${username}";
    private static final StatementIndexer changePassword = new StatementIndexer(
            changePasswordSql);

    @Override
    public void changePassword(String username, String newhash, String newsalt)
            throws UserManagementException, ServerBackendException {
        try (Connection conn = ds.getConnection();
                NamedPreparedStatement ps = changePassword
                        .prepareStatement(conn)) {
            ps.setString("pwhash", newhash);
            ps.setString("salt", newsalt);
            ps.setString("username", username);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0)
                throw new UserManagementException(String.format(
                        "User %s does not exist.", username));
        } catch (SQLException e) {
            throw new ServerBackendException(e);
        }
    }

}
