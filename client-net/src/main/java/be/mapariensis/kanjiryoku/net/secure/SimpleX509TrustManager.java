package be.mapariensis.kanjiryoku.net.secure;

import be.mapariensis.kanjiryoku.config.ConfigManager;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SimpleX509TrustManager implements X509TrustManager {
    private static final Logger log = LoggerFactory
            .getLogger(SimpleX509TrustManager.class);

    private final String destinationName;
    private final X509TrustManager defaultTm;
    private final X509TrustManager customTm;
    // TODO: move config constants to separate file
    public static final String AUTO_TRUST_LOCAL = "alwaysTrustLocal";
    public static final String SKIP_SYSTEM_TRUST = "noSystemTrust";
    public static final String TRUSTED_CAS = "trustedCAFile";

    private Boolean localAndShouldSkip = null;

    private boolean shouldSkip() {
        // either the config requested that local addresses be treated like any other,
        // or we've previously already determined whether or not this address is local
        if(localAndShouldSkip != null) {
            return localAndShouldSkip;
        }

        boolean isLocal;
        // check if the address is in local space
        // this might trigger a DNS lookup, but that's not a big deal (simply cache the result)
        try {
            InetAddress ia = InetAddress.getByName(destinationName);
            isLocal = ia.isLoopbackAddress() || ia.isLinkLocalAddress() || ia.isSiteLocalAddress();
        } catch (UnknownHostException e) {
            isLocal = false;
        }
        this.localAndShouldSkip = isLocal;
        return isLocal;
    }

    private static X509TrustManager extractFromFactory(TrustManagerFactory tmf) {
        return Arrays.stream(tmf.getTrustManagers())
                .filter(X509TrustManager.class::isInstance)
                .map(X509TrustManager.class::cast)
                .findFirst().orElse(null);
    }

    private static X509TrustManager fromTrustFile(String fileName) throws NoSuchAlgorithmException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");

        // generate a PKIX trust manager based on the CA chain of trust supplied
        try {
            FileInputStream fis = new FileInputStream(fileName);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Set<TrustAnchor> anchors = cf.generateCertificates(fis).stream()
                    .filter(X509Certificate.class::isInstance)
                    .map(cert -> new TrustAnchor((X509Certificate) cert, null))
                    .collect(Collectors.toUnmodifiableSet());
            PKIXBuilderParameters params = new PKIXBuilderParameters(anchors, null);
            tmf.init(new CertPathTrustManagerParameters(params));
            return extractFromFactory(tmf);
        } catch (IOException | CertificateException | InvalidAlgorithmParameterException ex) {
            log.warn(String.format("Could not read certificate file '%s'.", fileName), ex);
            return null;
        }
    }

    public SimpleX509TrustManager(String destinationName, ConfigManager m)
            throws BadConfigurationException, NoSuchAlgorithmException {
        IProperties config = m.getCurrentConfig();

        this.destinationName = destinationName;
        if(!config.getTyped(AUTO_TRUST_LOCAL, Boolean.class, true)) {
            // (site-)local connections should get X509 verification, so
            //  we can treat them like any other host
            this.localAndShouldSkip = false;
        }

        // get the system's default chain of trust
        TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance("PKIX");
        X509TrustManager defaultTm;
        boolean skipSystemTrust = config.getTyped(SKIP_SYSTEM_TRUST, Boolean.class, false);
        if(skipSystemTrust) {
            defaultTm = null;
        } else {
            try {
                defaultTmf.init((KeyStore) null);
                defaultTm = extractFromFactory(defaultTmf);
                if(defaultTm == null) {
                    log.warn("Could not load system trust info, received null.");
                }
            } catch (KeyStoreException e) {
                log.warn("Failed to load system trust info", e);
                defaultTm = null;
            }
        }

        // now load the ones from the file supplied
        String caTrustFile = config.getTyped(TRUSTED_CAS, String.class);
        X509TrustManager customTm = null;
        if(caTrustFile == null) {
            if(skipSystemTrust)
                throw new BadConfigurationException(
                        "You must supply a list of trusted CAs when disabling system trust");
        } else {
            customTm = fromTrustFile(caTrustFile);
            if (customTm == null) {
                log.warn("Could not load extra trust info, received null.");
            }
        }
        if(customTm == null && defaultTm == null)
            throw new BadConfigurationException("Could not load any trust information.");
        this.defaultTm = defaultTm;
        this.customTm = customTm;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if(customTm != null) {
            try {
                customTm.checkClientTrusted(chain, authType);
                // if we get here, the check passes and we don't need to fall through
                return;
            } catch (CertificateException ex) {
                log.debug("Custom trust chain did not accept certificate, fall back to default", ex);
            }
        }
        if (defaultTm != null) {
            defaultTm.checkClientTrusted(chain, authType);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        log.debug("Verifying server identity for " + destinationName);
        if(shouldSkip()) {
            log.info("Address is local, trusting automatically.");
            return;
        }
        // Verify common name
        // TODO surely this has been implemented somewhere,
        //  but I can't seem to find a simple drop-in implementation
        X509Certificate leaf = chain[0];
        String dn = leaf.getSubjectX500Principal().getName();
        // TODO check subjectAlternativeName entries?

        // LdapName implements a RFC 2253 parser
        LdapName ldapDN;
        try {
            ldapDN = new LdapName(dn);
        } catch (InvalidNameException e) {
            throw new CertificateException(
                    "Certificate DN not formatted according to RFC 2253");
        }
        String cn = null;
        for (Rdn rdn : ldapDN.getRdns()) {
            if ("CN".equals(rdn.getType())) {
                cn = rdn.getValue().toString();
            }
        }
        if (cn == null)
            throw new CertificateException("CN not set");
        if (!destinationName.matches(cn)) {
            String msg = String.format("CN %s does not match hostname %s.",
                    cn, destinationName);
            throw new CertificateException(msg);
        }

        if(customTm != null) {
            try {
                customTm.checkServerTrusted(chain, authType);
                // if we get here, the check passes and we don't need to fall through
                return;
            } catch (CertificateException ex) {
                log.debug("Custom trust chain did not accept certificate, fall back to default", ex);
            }
        }
        if (defaultTm != null) {
            defaultTm.checkServerTrusted(chain, authType);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        X509Certificate[] customCerts;
        if(customTm != null) {
            customCerts = customTm.getAcceptedIssuers();
        } else {
            customCerts = new X509Certificate[0];
        }
        X509Certificate[] defaultCerts;
        if(defaultTm != null) {
            defaultCerts = defaultTm.getAcceptedIssuers();
        } else {
            defaultCerts = new X509Certificate[0];
        }
        X509Certificate[] allCerts = new X509Certificate[customCerts.length + defaultCerts.length];
        System.arraycopy(customCerts, 0, allCerts, 0, customCerts.length);
        System.arraycopy(defaultCerts, 0, allCerts, customCerts.length, defaultCerts.length);
        return allCerts;
    }
}
