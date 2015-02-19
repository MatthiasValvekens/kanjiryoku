package be.mapariensis.kanjiryoku.dict;

import java.io.IOException;
import java.util.Set;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface KanjidicInterface extends AutoCloseable {
	public Set<String> getOn(char kanji) throws DictionaryAccessException;

	public Set<String> getKun(char kanji) throws DictionaryAccessException;

	public Set<Character> getKanjiByOn(String on)
			throws DictionaryAccessException;

	public Set<Character> getKanjiByKun(String kun)
			throws DictionaryAccessException;

	public Set<Character> getSimilar(char kanji)
			throws DictionaryAccessException;

	public Set<Character> randomKanji() throws DictionaryAccessException;

	public static interface Factory {
		public KanjidicInterface setUp(IProperties conf)
				throws DictionaryAccessException, BadConfigurationException;
	}

	@Override
	public void close() throws IOException;
}
