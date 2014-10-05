package be.mapariensis.kanjiryoku.config;

public class ConfigFields {
	public static final String CONFIG_FILE_NAME = "kanjiryoku.cfg";
	public static final String CONFIG_FILE_DIR = ".";
	
	/**
	 * The port number to listen on.
	 */
	public static final String PORT = "port";
	/**
	 * The number of worker threads (default 10). Adjust as necessary.
	 */
	public static final String WORKER_THREADS = "workerThreads";
	public static final int WORKER_THREADS_DEFAULT = 10;
	/**
	 * The size of the ByteBuffer attached to each worker thread (default 2KB).
	 */
	public static final String WORKER_BUFFER_SIZE = "workerBufferSize";
	public static final int WORKER_BUFFER_SIZE_DEFAULT = 2048;	
	
	public static final String USERNAME_LIMIT = "usernameCharLimit";
	public static final int USERNAME_LIMIT_DEFAULT = 10;
	
	public static final String ENABLE_ADMIN = "enableAdminCommands";
	public static final boolean ENABLE_ADMIN_DEFAULT = false;
	
	// CR-related settings
	
	public static final String CR_SETTINGS_HEADER = "engine";
	public static final String GUESSER_FACTORY_CLASS = "guesserFactoryClassName";	
	public static final String MODEL_FILE = "writingModelPath";
	public static final String CR_TOLERANCE = "answerTolerance";
	public static final int CR_TOLERANCE_DEFAULT = 50;
	
	// Game-specific settings
	public static final String GAME_SETTINGS_HEADER = "games";
	
	// TakingTurns
	public static final String PROBLEMS_PER_CATEGORY = "problemsPerCategory";
	public static final int PROBLEMS_PER_CATEGORY_DEFAULT = 5;
	public static final String RESET_AFTER_CATEGORY_SWITCH = "resetDifficultyAfterCategorySwitch";
	public static final String MIN_DIFFICULTY = "minimalDifficulty";
	public static final int MIN_DIFFICULTY_DEFAULT = 1;
	public static final String MAX_DIFFICULTY = "maximalDifficulty";
	public static final int MAX_DIFFICULTY_DEFAULT = 11;
	public static final String CATEGORY_LIST = "categories";
	public static final String FILE_NAME_FORMAT = "filePathPattern";
	public static final String FILE_NAME_DIFFICULTY_VAR = "difficulty";
	public static final String FILE_NAME_CATEGORY_VAR = "category";
	public static final String FILE_NAME_DIFFICULTY_FORMAT = "difficultyFormat";
	public static final String FILE_NAME_DIFFICULTY_FORMAT_DEFAULT = "%02d";
	public static final String ENABLE_BATON_PASS = "enableBatonPass";
	public static final boolean ENABLE_BATON_PASS_DEFAULT = false;
}

