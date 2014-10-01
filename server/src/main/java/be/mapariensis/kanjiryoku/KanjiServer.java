package be.mapariensis.kanjiryoku;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.config.IProperties;
import be.mapariensis.kanjiryoku.config.IPropertiesImpl;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.server.ConnectionMonitor;



public class KanjiServer {
	public static void main(String[] args) throws IOException, BadConfigurationException {
		IProperties props;
		String config;
		try {
			config = new String(Files.readAllBytes(Paths.get(ConfigFields.CONFIG_FILE_NAME)));
		} catch(Exception ex) {
			throw new IOException("Failed to read configuration file "+ConfigFields.CONFIG_FILE_NAME,ex);
		}
		//load configuration
		props= new IPropertiesImpl(config);
		ConnectionMonitor s = new ConnectionMonitor(props);
		s.start();
	}
}
