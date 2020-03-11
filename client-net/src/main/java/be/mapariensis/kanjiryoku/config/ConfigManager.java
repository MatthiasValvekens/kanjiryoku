package be.mapariensis.kanjiryoku.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import be.mapariensis.kanjiryoku.net.secure.SimpleX509TrustManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.util.IProperties;
import be.mapariensis.kanjiryoku.util.IPropertiesImpl;

public class ConfigManager {
	private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
	private final Path configFile;
	private JSONObject currentConfig;
	private final ExecutorService saver = Executors.newSingleThreadExecutor();

	public static final boolean SSL_DEFAULT = true;

	private class SaveThread implements Runnable {
		@Override
		public void run() {
			try (BufferedWriter bw = Files.newBufferedWriter(configFile,
					StandardCharsets.UTF_8)) {
				bw.write(currentConfig.toString());
			} catch (IOException e) {
				log.error("Failed to write to config file.", e);
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
			config = Files.readString(configFile);
		} catch (Exception ex) {
			throw new IOException("Failed to read configuration file "
					+ configFile.toString(), ex);
		}
		return config;
	}

	public ConfigManager(Path configFile) throws IOException {
		this.configFile = configFile;
		if (Files.exists(configFile)) {
			currentConfig = new JSONObject(readConfig());
		} else {
			currentConfig = new JSONObject();
		}
	}

	public <T> ConfigListener<T> watch(String key) {
		return new ConfigListener<>(key);
	}

	public IProperties getCurrentConfig() {
		return new IPropertiesImpl(currentConfig);
	}

	public SSLContext getSSLContext(String destination) throws IOException {
		try {
			SSLContext context = SSLContext.getInstance("TLS");
			TrustManager tm = new SimpleX509TrustManager(destination, this);
			// we're not presenting any certificates, so no key managers necessary
			context.init(new KeyManager[0], new TrustManager[] { tm }, null);
			log.info("SSL context initialised successfully.");
			return context;
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}
}
