package be.mapariensis.kanjiryoku.persistent;

import javax.sql.DataSource;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface DataSourceProvider {
	DataSource getDataSource(IProperties config)
			throws BadConfigurationException;
}
