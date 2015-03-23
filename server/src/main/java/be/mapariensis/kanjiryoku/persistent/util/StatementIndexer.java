package be.mapariensis.kanjiryoku.persistent.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatementIndexer {
	private static final Pattern varPattern = Pattern
			.compile("\\$\\{([a-z0-9-]+)\\}");
	private final Map<String, Integer> index = new HashMap<String, Integer>();
	private final String cleanedSql;

	public StatementIndexer(String sql) {
		Matcher m = varPattern.matcher(sql);
		// find things that look like variable names in the input string
		int count = 0;
		while (m.find()) {
			String varName = m.group(1);
			index.put(varName, ++count);
		}
		// clean up sql string
		m.reset();
		this.cleanedSql = m.replaceAll("?");
	}

	public NamedPreparedStatement prepareStatement(Connection conn)
			throws SQLException {
		return new NamedPreparedStatement(conn.prepareStatement(cleanedSql),
				index);
	}

	public String getSql() {
		return cleanedSql;
	}

	public Map<String, Integer> getIndex() {
		return Collections.unmodifiableMap(index);
	}
}
