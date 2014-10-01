package be.mapariensis.kanjiryoku.cr;

import java.io.IOException;

import be.mapariensis.kanjiryoku.config.IProperties;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;

public interface KanjiGuesserFactory {
	public KanjiGuesser getGuesser(IProperties config) throws BadConfigurationException, IOException;
}
