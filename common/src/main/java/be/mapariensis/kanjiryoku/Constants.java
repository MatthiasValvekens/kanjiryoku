package be.mapariensis.kanjiryoku;

public class Constants {
    public static final int majorVersion = 0;
    public static final int minorVersion = 1;
    public static final int patch = 1;

    public static String version() {
        return String.format("%d.%d.%d", majorVersion, minorVersion, patch);
    }

    public static final String SERVER_HANDLE = "*server*";
    public static final String SYSTEM_HANDLE = "*system*";
    public static final String COMMENT_PREFIX = "//";
}
