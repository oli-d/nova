/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

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

    private AsyncHttpClientFactory() {
    }

    public static AsyncHttpClientConfig.Builder builderFor (HttpClientSettings settings) {
        Objects.requireNonNull(settings, "HttpClientSettings must not be null");

        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder()
                .setRequestTimeout(settings.defaultRequestTimeoutInSeconds * 1000)
                .setCompressionEnforced(settings.compressionEnforced)
                .setConnectTimeout(settings.connectionTimeoutInSeconds * 1000)
                .setWebSocketTimeout(settings.webSocketTimeoutInSeconds * 1000)
                .setAcceptAnyCertificate(settings.acceptAnyCertificate);

        Optional.ofNullable(settings.userAgent).ifPresent(builder::setUserAgent);

        createSslContextFor(settings).ifPresent(builder::setSSLContext);

        return builder;
    }

    public static AsyncHttpClient clientFor(AsyncHttpClientConfig config) {
        return new AsyncHttpClient(Objects.requireNonNull(config, "AsyncHttpClientConfig must not be null"));
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
            throw new RuntimeException(sslFailure);
        }
    }

    private static SSLContext createSslContext(String sslKeyStorePath, String sslKeystorePassword) {
        try {
            if (isBlank(sslKeyStorePath)) {
                throw new IllegalArgumentException("The SSL keystore file path must be provided!");
            }
            char[] keypass = Optional.ofNullable(sslKeystorePassword).map(String::toCharArray).orElse(null);
            KeyStore keyStore = getDefaultKeyStore();
            keyStore.load(new FileInputStream(sslKeyStorePath), keypass);
            return createSslContextFor(keyStore);
        } catch (Exception sslFailure) {
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
