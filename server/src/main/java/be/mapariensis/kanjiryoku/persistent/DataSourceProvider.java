package be.mapariensis.kanjiryoku.persistent;

import javax.sql.DataSource;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface DataSourceProvider {
	public DataSource getDataSource(IProperties config)
			throws BadConfigurationException;
}
