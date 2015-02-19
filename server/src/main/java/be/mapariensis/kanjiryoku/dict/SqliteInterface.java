package be.mapariensis.kanjiryoku.dict;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;
import be.mapariensis.kanjiryoku.util.IPropertiesImpl;

public class SqliteInterface implements KanjidicInterface {
	private static final Logger log = LoggerFactory
			.getLogger(SqliteInterface.class);
	// TODO: allow for named parameters in queries
	public static final String SEARCH_BY_KUN = "searchByKun";
	public static final String SEARCH_BY_ON = "searchByOn";
	public static final String QUERY_ON_READINGS = "queryOnReadings";
	public static final String QUERY_KUN_READINGS = "queryKunReadings";
	public static final String SELECT_RANDOM = "selectRandom";
	public static final String FIND_SIMILAR = "findSimilar";
	public static final String DBFILE = "dbFile";
	public static final String QUERYFILE = "queryFile";

	public static class Factory implements KanjidicInterface.Factory {

		@Override
		public KanjidicInterface setUp(IProperties conf)
				throws DictionaryAccessException, BadConfigurationException {
			return new SqliteInterface(conf.getRequired(DBFILE, String.class),
					conf.getRequired(QUERYFILE, String.class));
		}

	}

	private final String repr;
	private final Connection conn;
	private final String searchByOn, searchByKun, queryOnReadings,
			queryKunReadings, findSimilar, selectRandom;

	protected SqliteInterface(String dbFile, String queryFile)
			throws BadConfigurationException, DictionaryAccessException {
		repr = String.format("[%s: dbFile=%s, queryFile=%s]",
				SqliteInterface.class.getCanonicalName(), dbFile, queryFile);
		// read queries
		String config;
		try {
			config = new String(Files.readAllBytes(Paths.get(queryFile)));
		} catch (Exception ex) {
			throw new BadConfigurationException("Failed to read query file "
					+ queryFile, ex);
		}

		IProperties queryConfig = new IPropertiesImpl(config);
		// register driver
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			log.warn("Standard sqlite driver not found", e);
		}

		// connect
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
		} catch (SQLException e) {
			throw new DictionaryAccessException(e);
		}

		// find queries
		searchByOn = queryConfig.getRequired(SEARCH_BY_ON, String.class);
		searchByKun = queryConfig.getRequired(SEARCH_BY_KUN, String.class);
		queryOnReadings = queryConfig.getRequired(QUERY_ON_READINGS,
				String.class);
		queryKunReadings = queryConfig.getRequired(QUERY_KUN_READINGS,
				String.class);
		findSimilar = queryConfig.getRequired(FIND_SIMILAR, String.class);
		selectRandom = queryConfig.getRequired(SELECT_RANDOM, String.class);
	}

	private PreparedStatement setUpQuery(String query)
			throws DictionaryAccessException {
		try {
			return conn.prepareStatement(query);
		} catch (SQLException e) {
			try {
				conn.close();
			} catch (SQLException e1) {
				log.error("Failed to close database connection during abort.",
						e1);
			}
			throw new DictionaryAccessException(e);
		}
	}

	// 1-parameter query which returns an 1-column table of strings
	protected Set<String> queryStringSet(String query, String param)
			throws DictionaryAccessException {
		try (PreparedStatement ps = setUpQuery(query)) {
			ps.setString(1, param);
			try (ResultSet res = ps.executeQuery()) {
				Set<String> results = new HashSet<String>();
				while (res.next()) {
					results.add(res.getString(1));
				}
				return results;
			}
		} catch (SQLException e) {
			throw new DictionaryAccessException(e);
		}
	}

	protected Set<Character> queryCharSet(String query, String param)
			throws DictionaryAccessException {
		Set<String> asStrings = queryStringSet(query, param);
		Set<Character> res = new HashSet<Character>();
		for (String s : asStrings) {
			if (s.length() > 1)
				throw new DictionaryAccessException(
						"Expected string of length 1, but got " + s);
			res.add(s.charAt(0));
		}
		return res;
	}

	@Override
	public Set<String> getOn(char kanji) throws DictionaryAccessException {
		return queryStringSet(queryOnReadings, Character.toString(kanji));
	}

	@Override
	public Set<String> getKun(char kanji) throws DictionaryAccessException {
		return queryStringSet(queryKunReadings, Character.toString(kanji));
	}

	@Override
	public Set<Character> getKanjiByOn(String on)
			throws DictionaryAccessException {
		return queryCharSet(searchByOn, on);
	}

	@Override
	public Set<Character> getKanjiByKun(String kun)
			throws DictionaryAccessException {
		return queryCharSet(searchByKun, kun);
	}

	@Override
	public Set<Character> getSimilar(char kanji)
			throws DictionaryAccessException {
		return queryCharSet(findSimilar, Character.toString(kanji));
	}

	@Override
	public void close() {
		silentClose(conn);
	}

	private static void silentClose(AutoCloseable... things) {
		for (AutoCloseable cl : things) {
			try {
				cl.close();
			} catch (Exception e) {
				log.error("Failed to close resource", e);
			}
		}
	}

	@Override
	public String toString() {
		return repr;
	}

	@Override
	public Set<Character> randomKanji() throws DictionaryAccessException {
		try (PreparedStatement ps = setUpQuery(selectRandom)) {
			try (ResultSet res = ps.executeQuery()) {
				Set<Character> results = new HashSet<Character>();
				while (res.next()) {
					String s = res.getString(1);
					if (s.length() > 1)
						throw new DictionaryAccessException(
								"Expected string of length 1, but got " + s);
					results.add(s.charAt(0));
				}
				return results;
			}
		} catch (SQLException e) {
			throw new DictionaryAccessException(e);
		}
	}
}
