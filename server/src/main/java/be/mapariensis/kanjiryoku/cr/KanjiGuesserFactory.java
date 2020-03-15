package be.mapariensis.kanjiryoku.cr;

import java.io.IOException;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface KanjiGuesserFactory {
	KanjiGuesser getGuesser(IProperties config)
			throws BadConfigurationException, IOException;
}
