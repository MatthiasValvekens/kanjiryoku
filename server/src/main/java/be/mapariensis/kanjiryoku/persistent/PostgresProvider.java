package be.mapariensis.kanjiryoku.persistent;

import javax.sql.DataSource;

import org.postgresql.ds.PGPoolingDataSource;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public class PostgresProvider implements DataSourceProvider {
	public static final String SERVER_NAME = "serverName";
	public static final String DATABASE_NAME = "databaseName";
	public static final String DATABASE_USER = "user";
	public static final String DATABASE_PASSWORD = "password";
	public static final String DATABASE_PORT = "portNumber";

	@Override
	public DataSource getDataSource(IProperties config)
			throws BadConfigurationException {
		PGPoolingDataSource ds = new PGPoolingDataSource();
		ds.setServerName(config.getRequired(SERVER_NAME, String.class));
		ds.setPortNumber(config.getRequired(DATABASE_PORT, Integer.class));
		ds.setDatabaseName(config.getRequired(DATABASE_NAME, String.class));
		ds.setUser(config.getRequired(DATABASE_USER, String.class));
		ds.setPassword(config.getRequired(DATABASE_PASSWORD, String.class));

		return ds;
	}
}
