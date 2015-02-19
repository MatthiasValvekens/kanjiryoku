package be.mapariensis.kanjiryoku.util;

import java.util.Set;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;

/**
 * Interface for reading a keyword-value dictionary containing objects of
 * various types.
 * 
 * @author Matthias Valvekens
 * @version 1.0
 */
public interface IProperties {
	/**
	 * Request the value associated with the specified key.
	 * 
	 * @param key
	 *            A key.
	 * @return The value associated with {@code key}. Return {@code null} if the
	 *         key in question was not found.
	 */
	public Object get(String key);

	/**
	 * Request the value associated with the specified key.
	 * 
	 * @param key
	 *            A key.
	 * @param defaultVal
	 *            A default value.
	 * @return The value associated with {@code key}. Return {@code defaultVal}
	 *         if the key in question was not found.
	 */
	public Object get(String key, Object defaultVal);

	/**
	 * Request the value associated with the specified key. The type restriction
	 * is enforced.
	 * 
	 * @param key
	 *            A key.
	 * @param type
	 *            A value type.
	 * @return The value associated with {@code key}. Return {@code null} if the
	 *         key in question was not found.
	 * @throws BadConfigurationException
	 *             No matching value exists.
	 */
	public <T> T getRequired(String key, Class<T> type)
			throws BadConfigurationException;

	/**
	 * Request the value associated with the specified key. The type restriction
	 * is enforced, but defaulting is allowed.
	 * 
	 * @param key
	 *            A key.
	 * @param type
	 *            A value type.
	 * @return The value associated with {@code key},
	 * @throws BadConfigurationException
	 *             The key exists, but its value could not be cast to
	 *             {@code type}.
	 */
	public <T> T getTyped(String key, Class<T> type)
			throws BadConfigurationException;

	/**
	 * Request the value associated with the specified key. The type restriction
	 * is enforced, but defaulting is allowed.
	 * 
	 * @param key
	 *            A key.
	 * @param type
	 *            A value type.
	 * @param defaultVal
	 *            A default value.
	 * @return The value associated with {@code key}. Return {@code defaultVal}
	 *         if the key in question was not found.
	 * @throws BadConfigurationException
	 *             The key exists, but its value could not be cast to
	 *             {@code type}.
	 */
	public <T> T getTyped(String key, Class<T> type, T defaultVal)
			throws BadConfigurationException;

	/**
	 * Request the value associated with the specified key, or a default value
	 * if the key in question could not be read for whatever reason.
	 * 
	 * @param key
	 *            A key.
	 * @param type
	 *            A value type.
	 * @param defaultVal
	 *            A default value.
	 * @return The value associated with {@code key}. Return {@code defaultVal}
	 *         if the key in question was not found or could not be cast to
	 *         {@code type}.
	 * 
	 */
	public <T> T getSafely(String key, Class<T> type, T defaultVal);

	/**
	 * Find and parse a time field.
	 * 
	 * @param key
	 *            A key.
	 * @param defaultVal
	 *            A default value (in Unix time)
	 * @return A Unix timestamp.
	 * @throws BadConfigurationException
	 *             The value could not be parsed.
	 */
	public long getTimeMillis(String key, long defaultVal)
			throws BadConfigurationException;

	/**
	 * Find and parse a time field.
	 * 
	 * @param key
	 *            A key.
	 * @return A Unix timestamp.
	 * @throws BadConfigurationException
	 *             The value could not be parsed or does not exist.
	 */
	public long getTimeMillis(String key) throws BadConfigurationException;

	/**
	 * Return a {@code Set} view of the keys in this IProperties object.
	 */
	public Set<String> keySet();

	/**
	 * Check whether a given key exists in this IProperties object.
	 * 
	 * @param key
	 *            A key.
	 */
	public boolean containsKey(String key);
}
