package be.mapariensis.kanjiryoku.net;

import java.nio.charset.Charset;

public class Constants {
	public static final Charset ENCODING = Charset.forName("UTF-8");
	public static final int BUFFER_MAX = 1024;
	public static final String GREETING = "Kanjiryoku server says hello. Version is "+be.mapariensis.kanjiryoku.Constants.version();
	
	public static final String COMMAND_REGISTER = "REGISTER";
	
	
	public static final String RESPONSE_SUCCESS = "SUCCESS";
	public static final String ACCEPTS = "ACCEPT";
	public static final String NONE = "NONE";
}
