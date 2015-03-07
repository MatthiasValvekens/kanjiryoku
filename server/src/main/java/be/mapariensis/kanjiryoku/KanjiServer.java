package be.mapariensis.kanjiryoku;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.config.ConfigFileWatcher;
import be.mapariensis.kanjiryoku.config.ServerConfigImpl;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.server.ConnectionMonitor;
import be.mapariensis.kanjiryoku.util.IPropertiesImpl;

public class KanjiServer {
	private static final Logger log = LoggerFactory
			.getLogger(KanjiServer.class);
	private static Path configFile;

	private static synchronized String readConfig() throws IOException {
		String config;
		try {
			config = new String(Files.readAllBytes(configFile));
		} catch (Exception ex) {
			throw new IOException("Failed to read configuration file "
					+ configFile.toString(), ex);
		}
		return config;
	}

	public static void main(String[] args) throws IOException,
			BadConfigurationException {
		// Check for config file on command line
		switch (args.length) {
		case 0:
			configFile = Paths.get(ConfigFields.CONFIG_FILE_NAME);
			break;
		case 1:
			configFile = Paths.get(args[0]);
			break;
		default:
			System.err
					.println(String
							.format("Too many arguments.\n"
									+ "You may optionally pass a configuration file name on the command line. "
									+ "The default is %s.",
									ConfigFields.CONFIG_FILE_NAME));
			return;
		}
		final ServerConfigImpl props = new ServerConfigImpl(
				new IPropertiesImpl(readConfig()));
		Runnable reconf = new Runnable() {

			@Override
			public void run() {
				log.info("Rereading configuration file");
				try {
					props.swapBackend(readConfig());
				} catch (BadConfigurationException | IOException e) {
					log.info("Failed to update config", e);
				}
			}
		};
		@SuppressWarnings("resource")
		ConnectionMonitor s = new ConnectionMonitor(props);
		ConfigFileWatcher watcher = new ConfigFileWatcher(reconf, configFile);
		watcher.start();
		s.start();
	}
}
