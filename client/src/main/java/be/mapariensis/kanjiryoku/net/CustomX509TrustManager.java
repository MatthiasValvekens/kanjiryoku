package be.mapariensis.kanjiryoku.net;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomX509TrustManager implements X509TrustManager {
	private static final Logger log = LoggerFactory
			.getLogger(CustomX509TrustManager.class);

	private final X509TrustManager myTm;
	private final X509TrustManager defaultTm;

	public CustomX509TrustManager(KeyStore ks) throws NoSuchAlgorithmException,
			KeyStoreException {
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(ks);
		X509TrustManager myTm = null;
		for (TrustManager tm : tmf.getTrustManagers()) {
			if (tm instanceof X509TrustManager) {
				myTm = (X509TrustManager) tm;
				break;
			}
		}
		if (myTm == null)
			throw new KeyStoreException("No useable trust material.");
		this.myTm = myTm;

		X509TrustManager defaultTm = null;
		tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init((KeyStore) null);
		for (TrustManager tm : tmf.getTrustManagers()) {
			if (tm instanceof X509TrustManager) {
				defaultTm = (X509TrustManager) tm;
				break;
			}
		}

		if (defaultTm == null)
			throw new KeyStoreException("No useable trust material.");
		this.defaultTm = defaultTm;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		try {
			myTm.checkClientTrusted(chain, authType);
		} catch (CertificateException e) {
			log.debug("Falling back to default trust store.");
			defaultTm.checkClientTrusted(chain, authType);
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		try {
			myTm.checkServerTrusted(chain, authType);
		} catch (CertificateException e) {
			log.debug("Falling back to default trust store.");
			defaultTm.checkServerTrusted(chain, authType);
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		X509Certificate[] mycerts = myTm.getAcceptedIssuers();
		X509Certificate[] dcerts = defaultTm.getAcceptedIssuers();
		X509Certificate[] allcerts = new X509Certificate[mycerts.length
				+ dcerts.length];
		System.arraycopy(mycerts, 0, allcerts, 0, mycerts.length);
		System.arraycopy(dcerts, 0, allcerts, mycerts.length, dcerts.length);
		return allcerts;
	}
}
