package be.mapariensis.kanjiryoku;

import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.imageio.ImageIO;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.FontUIResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigManager;
import be.mapariensis.kanjiryoku.gui.InitWindow;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;

public class Kanjiryoku {
	private static final Logger log = LoggerFactory.getLogger(Kanjiryoku.class);
	private static final String KEYSTORE_NAME = "kanjiryoku-client.jks";
	private static final String CONFIG_FILE_NAME = "kanjiclient.cfg";
	private static final char[] KEYSTORE_PASS = "Cw2krWlMoEOAeCIqJoeB"
			.toCharArray();
	public static final String ICON_FILE = "icon.png";
	public static final Image ICON;

	public static final String FONT_FAMILY = "Meiryo";
	public static final int FONT_SIZE = 15;

	static {
		Image thing = null;
		try (InputStream is = InitWindow.class.getClassLoader()
				.getResourceAsStream(ICON_FILE)) {
			thing = ImageIO.read(is);
		} catch (IOException | IllegalArgumentException e) {
			log.warn("Failed to read icon file", e);
		}
		ICON = thing;
	}

	private static SSLContext sslSetUp() throws IOException {
		log.info("Decrypting SSL key store...");

		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			try (InputStream in = Kanjiryoku.class
					.getResourceAsStream(KEYSTORE_NAME)) {
				ks.load(in, KEYSTORE_PASS);
			}

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, KEYSTORE_PASS);

			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance("SunX509");
			tmf.init(ks);

			// FIXME: Dummy trust manager for testing purposes.
			X509TrustManager tm = new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(kmf.getKeyManagers(), new TrustManager[] { tm }, null);
			log.info("SSL context initialised successfully.");
			return context;
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}

	public static void main(String[] args) {
		System.setProperty("javax.net.debug", "ssl");
		// Check for config file on command line
		Path configFile;
		switch (args.length) {
		case 0:
			configFile = Paths.get(CONFIG_FILE_NAME);
			break;
		case 1:
			configFile = Paths.get(args[0]);
			break;
		default:
			System.err
					.println(String
							.format("Too many arguments.\n"
									+ "You may optionally pass a configuration file name on the command line. "
									+ "The default is %s.", CONFIG_FILE_NAME));
			return;
		}
		// turn on antialiasing
		System.setProperty("swing.aatext", "true");

		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			log.warn("Nimbus L&F not found");
		}
		// set font
		FontUIResource fr = new javax.swing.plaf.FontUIResource(FONT_FAMILY,
				Font.PLAIN, FONT_SIZE);
		UIManager.getLookAndFeelDefaults().put("defaultFont", fr);
		FontUIResource boldfr = new javax.swing.plaf.FontUIResource(
				FONT_FAMILY, Font.BOLD, FONT_SIZE);
		UIManager.getLookAndFeelDefaults().put("Label.font", boldfr);

		try {
			SSLContext sslc = sslSetUp();
			InitWindow.show(new ConfigManager(configFile, sslc));
		} catch (BadConfigurationException | IOException e) {
			log.error("Failed to process configuration.", e);
			return;
		}
	}
}
