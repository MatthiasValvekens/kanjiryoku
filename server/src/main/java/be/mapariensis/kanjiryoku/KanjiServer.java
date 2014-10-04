package be.mapariensis.kanjiryoku;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.config.IPropertiesImpl;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.server.ConnectionMonitor;



public class KanjiServer {
	private static final Logger log = LoggerFactory.getLogger(KanjiServer.class);
	private static String readConfig() throws IOException {
		String config;
		try {
			config = new String(Files.readAllBytes(Paths.get(ConfigFields.CONFIG_FILE_NAME)));
		} catch(Exception ex) {
			throw new IOException("Failed to read configuration file "+ConfigFields.CONFIG_FILE_NAME,ex);
		}
		return config;
	}
	public static void main(String[] args) throws IOException, BadConfigurationException {
		final IPropertiesImpl props = new IPropertiesImpl(readConfig());
		ConnectionMonitor s = new ConnectionMonitor(props, new Runnable() {
			
			@Override
			public void run() {
				log.info("Rereading configuration file");
				try {
					props.swapBackend(readConfig());
				} catch (BadConfigurationException | IOException e) {
					log.info("Failed to update config",e);
				}
			}
		});
		s.start();
	}
}
