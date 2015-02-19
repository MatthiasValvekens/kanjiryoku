package be.mapariensis.kanjiryoku.dict;

import java.util.Set;

import be.mapariensis.kanjiryoku.util.IProperties;

public interface KanjidicInterface {
	public Set<String> getOn(char kanji) throws DictionaryAccessException;

	public Set<String> getKun(char kanji) throws DictionaryAccessException;

	public Set<Character> getKanjiByOn(String on)
			throws DictionaryAccessException;

	public Set<Character> getKanjiByKun(String kun)
			throws DictionaryAccessException;

	public static interface Factory {
		public KanjidicInterface setUp(IProperties conf)
				throws DictionaryAccessException;
	}
}
