package be.mapariensis.kanjiryoku.dict;

import java.util.Set;

import be.mapariensis.kanjiryoku.config.ServerConfig;

public interface KanjidicInterface {
	public Set<String> getOn(char kanji) throws DictionaryAccessException;

	public Set<String> getKun(char kanji) throws DictionaryAccessException;

	public Set<Character> getKanjiByOn(String on)
			throws DictionaryAccessException;

	public Set<Character> getKanjiByKun(String kun)
			throws DictionaryAccessException;

	public static interface Factory {
		public KanjidicInterface setUp(ServerConfig conf)
				throws DictionaryAccessException;
	}
}
