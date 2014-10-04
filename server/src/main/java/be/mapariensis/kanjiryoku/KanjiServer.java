package be.mapariensis.kanjiryoku;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.config.ConfigFileWatcher;
import be.mapariensis.kanjiryoku.config.IPropertiesImpl;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.server.ConnectionMonitor;



public class KanjiServer {
	private static final Logger log = LoggerFactory.getLogger(KanjiServer.class);
	private static final Path configFile = Paths.get(ConfigFields.CONFIG_FILE_DIR).resolve(ConfigFields.CONFIG_FILE_NAME);
	private static synchronized String readConfig() throws IOException {
		String config;
		try {
			config = new String(Files.readAllBytes(configFile));
		} catch(Exception ex) {
			throw new IOException("Failed to read configuration file "+ConfigFields.CONFIG_FILE_NAME,ex);
		}
		return config;
	}
	public static void main(String[] args) throws IOException, BadConfigurationException {
		final IPropertiesImpl props = new IPropertiesImpl(readConfig());
		Runnable reconf = new Runnable() {
			
			@Override
			public void run() {
				log.info("Rereading configuration file");
				try {
					props.swapBackend(readConfig());
				} catch (BadConfigurationException | IOException e) {
					log.info("Failed to update config",e);
				}
			}
		};
		ConnectionMonitor s = new ConnectionMonitor(props, reconf);
		ConfigFileWatcher watcher = new ConfigFileWatcher(reconf);
		watcher.start();
		s.start();
	}
}
