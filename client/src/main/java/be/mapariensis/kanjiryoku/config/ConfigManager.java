package be.mapariensis.kanjiryoku.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.Kanjiryoku;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;
import be.mapariensis.kanjiryoku.util.IPropertiesImpl;

public class ConfigManager {
	private static final Logger log = LoggerFactory.getLogger(Kanjiryoku.class);
	private final Path configFile;
	private JSONObject currentConfig;
	private final ExecutorService saver = Executors.newSingleThreadExecutor();
	private final SSLContext sslc;

	private class SaveThread implements Runnable {
		@Override
		public void run() {
			try (BufferedWriter bw = Files.newBufferedWriter(configFile,
					Charset.forName("UTF-8"))) {
				bw.write(currentConfig.toString());
			} catch (IOException e) {
				log.error("Failed to write to config file.", e);
				return;
			}
		}
	}

	public class ConfigListener<T> {
		public final String key;

		protected ConfigListener(String key) {
			this.key = key;
		}

		public synchronized void changed(T newval) {
			currentConfig.put(key, newval);
			saver.execute(new SaveThread());
		}
	}

	private String readConfig() throws IOException {
		String config;
		try {
			config = new String(Files.readAllBytes(configFile),
					Charset.forName("UTF-8"));
		} catch (Exception ex) {
			throw new IOException("Failed to read configuration file "
					+ configFile.toString(), ex);
		}
		return config;
	}

	public ConfigManager(Path configFile, SSLContext sslc)
			throws BadConfigurationException, IOException {
		this.configFile = configFile;
		if (Files.exists(configFile)) {
			currentConfig = new JSONObject(readConfig());
		} else {
			currentConfig = new JSONObject();
		}
		this.sslc = sslc;
	}

	public <T> ConfigListener<T> watch(String key) {
		return new ConfigListener<T>(key);
	}

	public IProperties getCurrentConfig() {
		return new IPropertiesImpl(currentConfig);
	}

	public SSLContext getSSLContext() {
		return sslc;
	}
}
