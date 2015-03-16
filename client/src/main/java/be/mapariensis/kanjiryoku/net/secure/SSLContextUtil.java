package be.mapariensis.kanjiryoku.net.secure;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.Kanjiryoku;

public class SSLContextUtil {
	private static final String KEYSTORE_NAME = "kanjiryoku-client.jks";
	private static final char[] KEYSTORE_PASS = "Cw2krWlMoEOAeCIqJoeB"
			.toCharArray();
	private static final Logger log = LoggerFactory
			.getLogger(SSLContextUtil.class);

	public static SSLContext sslSetUp(String destinationName)
			throws IOException {
		log.info("Decrypting SSL key store...");

		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			try (InputStream in = Kanjiryoku.class.getClassLoader()
					.getResourceAsStream(KEYSTORE_NAME)) {
				if (in == null)
					throw new IOException("Keystore not found");
				ks.load(in, KEYSTORE_PASS);
			}

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, KEYSTORE_PASS);

			SSLContext context = SSLContext.getInstance("TLS");
			TrustManager tm = new CustomX509TrustManager(ks, destinationName);
			context.init(kmf.getKeyManagers(), new TrustManager[] { tm }, null);
			log.info("SSL context initialised successfully.");
			return context;
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}

}
