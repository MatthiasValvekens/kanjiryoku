package be.mapariensis.kanjiryoku.config;

import java.util.Collections;
import java.util.List;

public class ConfigFields {
    public static final String CONFIG_FILE_NAME = "./kanjiryoku.cfg";

    /**
     * The port number to listen on.
     */
    public static final String PORT = "port";
    /**
     * The number of worker threads (default 15). Adjust as necessary.
     */
    public static final String WORKER_THREADS = "workerThreads";
    public static final int WORKER_THREADS_DEFAULT = 15;
    /**
     * The size of the ByteBuffer attached to each plaintext message handler
     * (default 4KB).
     */
    public static final String PLAINTEXT_BUFFER_SIZE = "plaintextBufferSize";
    public static final int PLAINTEXT_BUFFER_SIZE_DEFAULT = 4096;

    /**
     * Controls whether SSL mode is enforced. Default is false for now, but this
     * *will* change in the future.
     */
    public static final String FORCE_SSL = "disablePlaintext";
    public static final boolean FORCE_SSL_DEFAULT = false;

    /**
     * Deliberately choose a cipher suite that Wireshark can intercept without issues.
     */
    public static final String DEBUG_SSL = "sslDebug";
    public static final boolean DEBUG_SSL_DEFAULT = false;

    public static final String USERNAME_LIMIT = "usernameCharLimit";
    public static final int USERNAME_LIMIT_DEFAULT = 10;

    public static final String SERVER_GREETING = "serverGreeting";

    public static final String ENABLE_ADMIN = "enableAdminCommands";
    public static final boolean ENABLE_ADMIN_DEFAULT = false;

    public static final String ADMIN_WHITELIST = "adminWhitelist";
    public static final List<String> ADMIN_WHITELIST_DEFAULT = Collections
            .emptyList();

    // CR-related settings

    public static final String CR_SETTINGS_HEADER = "engine";
    public static final String GUESSER_FACTORY_CLASS = "guesserFactoryClassName";
    public static final String MODEL_FILE = "writingModelPath";
    public static final String CR_TOLERANCE = "answerTolerance";
    public static final int CR_TOLERANCE_DEFAULT = 50;

    // Pooling settings
    public static final String MAX_IDLE = "maxIdle";
    public static final String MAX_TOTAL = "maxTotal";
    public static final String MIN_IDLE = "minIdle";
    public static final String MAX_WAIT = "poolTimeout";
    public static final String BACKEND_FACTORY = "backendFactory";
    public static final String BACKEND_CONFIG = "backendConfig";

    // Problem sets
    public static final String PSET_SETTINGS_HEADER = "problemSets";
    public static final String PARSER_FACTORY_CLASS = "parserFactoryClassName";
    public static final String PARSER_FACTORY_SETTINGS = "parserFactoryConfig";
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
    public static final String FILE_ENCODING = "fileEncoding";
    public static final String FILE_ENCODING_DEFAULT = "UTF-8";

    // Game-specific settings
    public static final String GAME_SETTINGS_HEADER = "games";

    // TakingTurns

    public static final String ENABLE_BATON_PASS = "enableBatonPass";
    public static final boolean ENABLE_BATON_PASS_DEFAULT = false;

    // DB settings
    public static final String DB_CONFIG = "dbConfig";
    public static final String DATA_SOURCE_PROVIDER = "dataSourceProvider";

    // Scoring settings
    public static final String SCORING_CONFIG = "scoring";
    public static final String SCORING_BACKEND_CLASS = "backendFactory";

    // Auth
    public static final String REQUIRE_AUTH = "requireAuthentication";
    public static final boolean REQUIRE_AUTH_DEFAULT = true;
    public static final String AUTH_BACKEND = "authBackend";
    public static final String AUTH_BACKEND_CONFIG = "config";
    public static final String AUTH_BACKEND_PROVIDER_CLASS = "providerFactory";
    public static final String SSL_AUTH_SUFFICIENT = "sslAuthSufficient";
    public static final boolean SSL_AUTH_SUFFICIENT_DEFAULT = true;
}
