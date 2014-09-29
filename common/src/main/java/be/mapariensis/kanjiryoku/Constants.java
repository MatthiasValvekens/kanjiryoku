package be.mapariensis.kanjiryoku;

public class Constants {
	public static final int majorVersion = 0;
	public static final int minorVersion = 1;
	public static final int patch = 0;
	public static final int TOLERANCE = 20;
	public static String version() {
		return String.format("%d.%d.%d",majorVersion,minorVersion,patch);
	}
}
