package be.mapariensis.kanjiryoku.net;

import java.nio.charset.Charset;

import be.mapariensis.kanjiryoku.net.model.ServerCommand;

public class Constants {
	public static final Charset ENCODING = Charset.forName("UTF-8");
	public static final int BUFFER_MAX = 1024;
	public static final String GREETING = "Kanjiryoku server says hello.\n" +
			"Registration command is "+ServerCommand.REGISTER+". Disconnect command is "+ServerCommand.BYE +
			".\nVersion "+be.mapariensis.kanjiryoku.Constants.version();
	
	public static final String COMMAND_REGISTER = "REGISTER";
	public static final String WELCOME = "Welcome";
	
	
	public static final String RESPONSE_SUCCESS = "SUCCESS";
	public static final String ACCEPTS = "ACCEPT";
	public static final String REJECTS = "REJECT";
	public static final String NONE = "NONE";
}
