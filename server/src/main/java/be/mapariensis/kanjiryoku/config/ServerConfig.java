package be.mapariensis.kanjiryoku.config;

import java.util.Set;

import be.mapariensis.kanjiryoku.cr.KanjiGuesserFactory;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.problemsets.ProblemSetManager;

public interface ServerConfig {

	public Object get(String key);

	public Object get(String key, Object defaultVal);

	public <T> T getRequired(String key, Class<T> type)
			throws BadConfigurationException;

	public <T> T getTyped(String key, Class<T> type)
			throws BadConfigurationException;

	public <T> T getTyped(String key, Class<T> type, T defaultVal)
			throws BadConfigurationException;

	public <T> T getSafely(String key, Class<T> type, T defaultVal);

	public long getTimeMillis(String key, long defaultVal)
			throws BadConfigurationException;

	public long getTimeMillis(String key) throws BadConfigurationException;

	public Set<String> keySet();

	public boolean containsKey(String key);

	public ProblemSetManager getProblemSetManager();

	public KanjiGuesserFactory getKanjiGuesserFactory();

}