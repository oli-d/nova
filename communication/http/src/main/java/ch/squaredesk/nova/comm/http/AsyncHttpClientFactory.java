package ch.squaredesk.nova.comm.http;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Objects;
import java.util.Optional;

public class AsyncHttpClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(AsyncHttpClientFactory.class);

    public static AsyncHttpClient clientFor(HttpClientSettings settings) {
        Objects.requireNonNull(settings, "HttpClientSettings must not be null");

        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder()
                .setRequestTimeout(settings.defaultRequestTimeoutInSeconds * 1000)
                .setCompressionEnforced(settings.compressionEnforced)
                .setConnectTimeout(settings.connectionTimeoutInSeconds * 1000)
                .setWebSocketTimeout(settings.webSocketTimeoutInSeconds * 1000)
                .setAcceptAnyCertificate(settings.acceptAnyCertificate);

        Optional.ofNullable(settings.userAgent).ifPresent(userAgent -> builder.setUserAgent(userAgent));

        createSslContextFor(settings).ifPresent(sslContext -> builder.setSSLContext(sslContext));

        return new AsyncHttpClient(builder.build());
    }

    private static Optional<SSLContext> createSslContextFor (HttpClientSettings settings) {
        if (isNotBlank(settings.sslCertificateContent)) {
            return Optional.of(createSslContextForCertificateContent(settings.sslCertificateContent));
        } else if (isNotBlank(settings.sslKeyStorePath)){
            return Optional.of(createSslContext(settings.sslKeyStorePath, settings.sslKeyStorePass));
        }

        return Optional.empty();
    }

    private static boolean isBlank(String string) {
        return string == null || string.trim().length() == 0;
    }

    private static boolean isNotBlank(String string) {
        return !isBlank(string);
    }

    private static SSLContext createSslContextForCertificateContent(String sslCertificateContent) {
        try {
            if (isBlank(sslCertificateContent)) {
                throw new IllegalArgumentException("The SSL certificate content must be provided!");
            }
            Certificate certificate = CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(sslCertificateContent.getBytes()));
            KeyStore keyStore = getDefaultKeyStore();
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", certificate);
            return createSslContextFor(keyStore);
        } catch (Exception sslFailure) {
            logger.error("Failed to create client with certificate: {}", sslFailure.getMessage());
            throw new RuntimeException(sslFailure);
        }
    }

    private static SSLContext createSslContext(String sslKeyStorePath, String sslKeystorePassword) {
        try {
            if (isBlank(sslKeyStorePath)) {
                throw new IllegalArgumentException("The SSL keystore file path must be provided!");
            }
            char[] keypass = Optional.ofNullable(sslKeystorePassword).map(pwd -> pwd.toCharArray()).orElse(null);
            KeyStore keyStore = getDefaultKeyStore();
            keyStore.load(new FileInputStream(sslKeyStorePath), keypass);
            return createSslContextFor(keyStore);
        } catch (Exception sslFailure) {
            logger.error("Failed to create client with keystore: {}", sslFailure.getMessage());
            throw new RuntimeException(sslFailure);
        }
    }

    private static KeyStore getDefaultKeyStore() throws KeyStoreException {
        return KeyStore.getInstance(KeyStore.getDefaultType());
    }

    private static SSLContext createSslContextFor(KeyStore keyStore)
            throws KeyStoreException, KeyManagementException, NoSuchAlgorithmException {
        TrustManagerFactory trustFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(keyStore);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustFactory.getTrustManagers(), null);
        return context;
    }
}
