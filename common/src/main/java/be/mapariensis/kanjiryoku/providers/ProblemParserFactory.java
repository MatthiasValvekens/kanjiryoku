package be.mapariensis.kanjiryoku.providers;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface ProblemParserFactory {
	public ProblemParser getParser(IProperties params) throws BadConfigurationException;
}
