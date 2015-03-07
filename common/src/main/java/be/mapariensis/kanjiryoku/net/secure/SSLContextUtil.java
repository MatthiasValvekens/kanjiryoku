package be.mapariensis.kanjiryoku.net.secure;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public class SSLContextUtil {
	private static final Logger log = LoggerFactory
			.getLogger(SSLContextUtil.class);

	public static final String KEYSTORE = "keyStore";
	public static final String TRUSTSTORE = "trustStore";
	public static final String KEYSTORE_PASS = "keyStorePassphrase";
	public static final String KEY_PASS = "keyPassphrase";
	public static final String TRUSTSTORE_PASS = "trustStorePassphrase";

	public static SSLContext setUp(IProperties config)
			throws BadConfigurationException, IOException {
		log.info("Decrypting SSL key stores...");
		char[] kspass = config.getRequired(KEYSTORE_PASS, String.class)
				.toCharArray();
		char[] tspass = config.getRequired(TRUSTSTORE_PASS, String.class)
				.toCharArray();
		char[] keypass = config.getRequired(KEY_PASS, String.class)
				.toCharArray();
		String ksfile = config.getRequired(KEYSTORE, String.class);
		String tsfile = config.getRequired(TRUSTSTORE, String.class);
		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			KeyStore ts = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream(ksfile), kspass);
			ts.load(new FileInputStream(tsfile), tspass);

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, keypass);

			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance("SunX509");
			tmf.init(ts);

			SSLContext context = SSLContext.getInstance("TLS");
			context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			log.info("SSL context initialised successfully.");
			return context;
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}
}
