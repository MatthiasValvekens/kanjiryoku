package be.mapariensis.kanjiryoku.net;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;

public class Constants {
	public static final int protocolMajorVersion = 1;
	public static final int protocolMinorVersion = 1;
	public static final Charset ENCODING = StandardCharsets.UTF_8;
	public static final String GREETING = "Kanjiryoku server says hello.\n"
			+ "Registration command is " + ServerCommandList.REGISTER
			+ ". Disconnect command is " + ServerCommandList.BYE + ".";

	public static final String MODE_TLS = "TLS";
	public static final String MODE_PLAIN = "PLAIN";

	public static final String ACCEPTS = "ACCEPT";
	public static final String REJECTS = "REJECT";
	public static final String NONE = "NONE";
	public static final int STANDARD_MACRO_TIMEOUT = 10000;

	public static final String GAMELIST_JSON_NAME = "name";
	public static final String GAMELIST_JSON_HUMANNAME = "humanName";
}
