package be.mapariensis.kanjiryoku.config;

import static be.mapariensis.kanjiryoku.config.ConfigFields.CATEGORY_LIST;
import static be.mapariensis.kanjiryoku.config.ConfigFields.FILE_ENCODING;
import static be.mapariensis.kanjiryoku.config.ConfigFields.FILE_ENCODING_DEFAULT;
import static be.mapariensis.kanjiryoku.config.ConfigFields.FILE_NAME_DIFFICULTY_FORMAT;
import static be.mapariensis.kanjiryoku.config.ConfigFields.FILE_NAME_DIFFICULTY_FORMAT_DEFAULT;
import static be.mapariensis.kanjiryoku.config.ConfigFields.FILE_NAME_FORMAT;
import static be.mapariensis.kanjiryoku.config.ConfigFields.MAX_DIFFICULTY;
import static be.mapariensis.kanjiryoku.config.ConfigFields.MAX_DIFFICULTY_DEFAULT;
import static be.mapariensis.kanjiryoku.config.ConfigFields.MIN_DIFFICULTY;
import static be.mapariensis.kanjiryoku.config.ConfigFields.MIN_DIFFICULTY_DEFAULT;
import static be.mapariensis.kanjiryoku.config.ConfigFields.PROBLEMS_PER_CATEGORY;
import static be.mapariensis.kanjiryoku.config.ConfigFields.PROBLEMS_PER_CATEGORY_DEFAULT;
import static be.mapariensis.kanjiryoku.config.ConfigFields.PSET_SETTINGS_HEADER;
import static be.mapariensis.kanjiryoku.config.ConfigFields.RESET_AFTER_CATEGORY_SWITCH;

import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.KanjiGuesserFactory;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.server.ConnectionMonitor;
import be.mapariensis.kanjiryoku.problemsets.ProblemSetManager;
import be.mapariensis.kanjiryoku.problemsets.ProblemSetManagerImpl;
import be.mapariensis.kanjiryoku.util.IProperties;
import be.mapariensis.kanjiryoku.util.IPropertiesImpl;

public class ServerConfigImpl implements ServerConfig {
	private static final Logger log = LoggerFactory
			.getLogger(ServerConfigImpl.class);
	private final IPropertiesImpl backend;
	private KanjiGuesserFactory kgf;
	private ProblemSetManager psm;
	private final Object LOCK = new Object();

	public ServerConfigImpl(IPropertiesImpl config)
			throws BadConfigurationException {
		this.backend = config;
		loadExtra();
	}

	public void swapBackend(String json) throws BadConfigurationException {
		synchronized (LOCK) {
			backend.swapBackend(json);
			loadExtra();
		}
	}

	public void swapBackend(JSONObject json) throws BadConfigurationException {
		synchronized (LOCK) {
			backend.swapBackend(json);
			loadExtra();
		}
	}

	@Override
	public Object get(String key) {
		return backend.get(key);
	}

	@Override
	public Object get(String key, Object defaultVal) {
		return backend.get(key, defaultVal);
	}

	@Override
	public <T> T getRequired(String key, Class<T> type)
			throws BadConfigurationException {
		return backend.getRequired(key, type);
	}

	@Override
	public <T> T getTyped(String key, Class<T> type)
			throws BadConfigurationException {
		return backend.getTyped(key, type);
	}

	@Override
	public <T> T getTyped(String key, Class<T> type, T defaultVal)
			throws BadConfigurationException {
		return backend.getTyped(key, type, defaultVal);
	}

	@Override
	public <T> T getSafely(String key, Class<T> type, T defaultVal) {
		return backend.getSafely(key, type, defaultVal);
	}

	@Override
	public long getTimeMillis(String key, long defaultVal)
			throws BadConfigurationException {
		return backend.getTimeMillis(key, defaultVal);
	}

	@Override
	public long getTimeMillis(String key) throws BadConfigurationException {
		return backend.getTimeMillis(key);
	}

	@Override
	public Set<String> keySet() {
		return backend.keySet();
	}

	@Override
	public boolean containsKey(String key) {
		return backend.containsKey(key);
	}

	@Override
	public ProblemSetManager getProblemSetManager() {
		return psm;
	}

	@Override
	public KanjiGuesserFactory getKanjiGuesserFactory() {
		return kgf;

	}

	private void loadExtra() throws BadConfigurationException {
		// load guesser factory
		IProperties crSettings = getRequired(ConfigFields.CR_SETTINGS_HEADER,
				IProperties.class);
		KanjiGuesserFactory factory;
		String className = crSettings.getRequired(
				ConfigFields.GUESSER_FACTORY_CLASS, String.class);
		try {
			log.info("Loading guesser factory {}", className);
			factory = (KanjiGuesserFactory) ConnectionMonitor.class
					.getClassLoader().loadClass(className).newInstance();
		} catch (Exception ex) {
			throw new BadConfigurationException(
					"Failed to instantiate guesser factory.", ex);
		}
		kgf = factory;

		log.info("Reloading problem set manager...");
		// load problem set manager
		IProperties psmSettings = getRequired(PSET_SETTINGS_HEADER,
				IProperties.class);
		int problemsPerCategory = psmSettings.getSafely(PROBLEMS_PER_CATEGORY,
				Integer.class, PROBLEMS_PER_CATEGORY_DEFAULT);
		boolean resetDifficulty = psmSettings.getSafely(
				RESET_AFTER_CATEGORY_SWITCH, Boolean.class, true);
		int minDiff = psmSettings.getSafely(MIN_DIFFICULTY, Integer.class,
				MIN_DIFFICULTY_DEFAULT);
		int maxDiff = psmSettings.getSafely(MAX_DIFFICULTY, Integer.class,
				MAX_DIFFICULTY_DEFAULT);
		String fileNameFormat = psmSettings.getRequired(FILE_NAME_FORMAT,
				String.class);
		String digitFormat = psmSettings.getSafely(FILE_NAME_DIFFICULTY_FORMAT,
				String.class, FILE_NAME_DIFFICULTY_FORMAT_DEFAULT);
		String enc = psmSettings.getSafely(FILE_ENCODING, String.class,
				FILE_ENCODING_DEFAULT);
		psm = new ProblemSetManagerImpl(fileNameFormat, digitFormat,
				problemsPerCategory, resetDifficulty, minDiff, maxDiff, enc);

		// load problem sets

		IProperties psconfig = psmSettings.getRequired(CATEGORY_LIST,
				IProperties.class);

		// iterate over the names of all problem sets
		for (String name : psconfig.keySet()) {
			psm.loadNewConfig(name,
					psconfig.getRequired(name, IProperties.class));
		}
	}

}
