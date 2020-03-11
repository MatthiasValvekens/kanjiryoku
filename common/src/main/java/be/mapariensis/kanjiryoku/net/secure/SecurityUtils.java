package be.mapariensis.kanjiryoku.net.secure;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.ByteUtils;
import be.mapariensis.kanjiryoku.util.IProperties;

public class SecurityUtils {
	private static final Logger log = LoggerFactory
			.getLogger(SecurityUtils.class);

	public static final String KEYSTORE = "keyStore";
	public static final String KEYSTORE_PASS = "keyStorePassphrase";
	public static final String KEY_PASS = "keyPassphrase";

	public static SSLContext setUp(IProperties config)
			throws BadConfigurationException, IOException {
		log.info("Decrypting SSL key stores...");
		char[] kspass = config.getRequired(KEYSTORE_PASS, String.class)
				.toCharArray();
		char[] keypass = config.getRequired(KEY_PASS, String.class)
				.toCharArray();
		String ksfile = config.getRequired(KEYSTORE, String.class);
		try {
			KeyStore ks = KeyStore.getInstance("JKS");

			try (InputStream in = new FileInputStream(ksfile)) {
				ks.load(in, kspass);
			}

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, keypass);

			SSLContext context = SSLContext.getInstance("TLS");
			// no client auth, so no trust managers needed
			context.init(kmf.getKeyManagers(), new TrustManager[0], null);
			log.info("SSL context initialised successfully.");
			return context;
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}

	public static byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(data);
		return md.digest();
	}

	public static String sha256(String data) throws NoSuchAlgorithmException {
		return ByteUtils.bytesToHex(sha256(data.getBytes(Constants.ENCODING)));
	}
}
