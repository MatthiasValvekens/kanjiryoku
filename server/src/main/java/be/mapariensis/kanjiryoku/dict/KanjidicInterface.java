package be.mapariensis.kanjiryoku.dict;

import java.io.IOException;
import java.util.Set;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

/**
 * Interface to implement kanji dictionary access.
 * 
 * @author Matthias Valvekens
 * @version 1.0
 */
public interface KanjidicInterface extends AutoCloseable {
	/**
	 * Retrieve the on reading of a kanji from the dictionary.
	 * 
	 * @param kanji
	 *            A kanji.
	 * @throws DictionaryAccessException
	 *             Thrown on failure to access the dictionary.
	 * @return The set of kun readings for this kanji.
	 */
	public Set<String> getOn(char kanji) throws DictionaryAccessException;

	/**
	 * Retrieve the kun reading of a kanji from the dictionary.
	 * 
	 * @param kanji
	 *            A kanji.
	 * @throws DictionaryAccessException
	 *             Thrown on failure to access the dictionary.
	 * @return The set of kun readings for this kanji.
	 */
	public Set<String> getKun(char kanji) throws DictionaryAccessException;

	/**
	 * Retrieve a number of kanji by on reading.
	 * 
	 * @param on
	 *            The on reading of a kanji. The convention used is left up to
	 *            the implementation. (options: kana, nihonshiki, kunreishiki)
	 * @return A set of kanji with the given on reading. The number of kanji
	 *         returned is left up to the implementation.
	 * @throws DictionaryAccessException
	 *             Thrown on failure to access the dictionary.
	 */
	public Set<Character> getKanjiByOn(String on)
			throws DictionaryAccessException;

	/**
	 * Retrieve a number of kanji by on reading.
	 * 
	 * @param on
	 *            The kun reading of a kanji. The convention used is left up to
	 *            the implementation. (options: kana, nihonshiki, kunreishiki)
	 * @return A set of kanji with the given kun reading. The number of kanji
	 *         returned is left up to the implementation.
	 * @throws DictionaryAccessException
	 *             Thrown on failure to access the dictionary.
	 */
	public Set<Character> getKanjiByKun(String kun)
			throws DictionaryAccessException;

	/**
	 * Retrieve a number similar kanji.
	 * 
	 * @param kanji
	 *            A kanji.
	 * @return A set of kanji that are somehow similar to the supplied kanji.
	 *         The similarity criterion and number of kanji returned is left up
	 *         to the implementation.
	 * @throws DictionaryAccessException
	 *             Thrown on failure to access the dictionary.
	 */
	public Set<Character> getSimilar(char kanji)
			throws DictionaryAccessException;

	/**
	 * Retrieve a random set of kanji.
	 * 
	 * @return A set of kanji. The number of kanji returned is left up to the
	 *         implementation.
	 * @throws DictionaryAccessException
	 *             Thrown on failure to access the dictionary.
	 */
	public Set<Character> randomKanji() throws DictionaryAccessException;

	/**
	 * Factory interface for KanjidicInterface. Implementations should provide a
	 * no-argument constructor.
	 * 
	 * @author Matthias Valvekens
	 * @version 1.0
	 */
	public static interface Factory {
		public KanjidicInterface setUp(IProperties conf)
				throws DictionaryAccessException, BadConfigurationException;
	}

	/**
	 * Close the handle on the dictionary.
	 */
	@Override
	public void close() throws IOException;
}
