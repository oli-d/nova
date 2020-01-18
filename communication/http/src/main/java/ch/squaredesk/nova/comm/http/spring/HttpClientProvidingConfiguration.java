/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.AsyncHttpClientFactory;
import ch.squaredesk.nova.comm.http.HttpClientSettings;
import com.ning.http.client.AsyncHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Optional;

@Configuration
public class HttpClientProvidingConfiguration {
    public interface BeanIdentifiers {
        String COMPRESSION_ENFORCED = "NOVA.HTTP.CLIENT.COMPRESSION_ENFORCED";
        String CONNECTION_TIMEOUT_IN_SECONDS = "NOVA.HTTP.CLIENT.CONNECTION_TIMEOUT_IN_SECONDS";
        String DEFAULT_REQUEST_TIMEOUT_IN_SECONDS = "NOVA.HTTP.CLIENT.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS";
        String WEB_SOCKET_TIMEOUT_IN_SECONDS = "NOVA.HTTP.CLIENT.WEB_SOCKET_TIMEOUT_IN_SECONDS";

        String CERTIFICATE = "NOVA.HTTP.CLIENT.CERTIFICATE";
        String KEY_STORE = "NOVA.HTTP.CLIENT.KEY_STORE";
        String KEY_STORE_PASSWORD = "NOVA.HTTP.CLIENT.KEY_STORE_PASSWORD";
        String KEY_STORE_PASSWORD_FILE = "NOVA.HTTP.CLIENT.KEY_STORE_PASSWORD_FILE";

        String SETTINGS = "NOVA.HTTP.CLIENT.SETTINGS";
        String AUTO_CREATE_CLIENT = "NOVA.HTTP.CLIENT.AUTO_CREATE";
        String CLIENT = "NOVA.HTTP.CLIENT";
        String CLIENT_NOTIFIER = "NOVA.HTTP.CLIENT.NOTIFIER";
    }

    @Bean(BeanIdentifiers.SETTINGS)
    HttpClientSettings httpClientSettings(
            @Autowired(required = false) Nova nova,
            @Qualifier(BeanIdentifiers.COMPRESSION_ENFORCED) boolean compressionEnforced,
            @Qualifier(BeanIdentifiers.CONNECTION_TIMEOUT_IN_SECONDS) int connectionTimeoutInSeconds,
            @Qualifier(BeanIdentifiers.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS) int defaultRequestTimeoutInSeconds,
            @Qualifier(BeanIdentifiers.WEB_SOCKET_TIMEOUT_IN_SECONDS) int webSocketTimeoutInSeconds,
            @Autowired(required = false) @Qualifier("NOVA.HTTP.CLIENT.ACCEPT_ANY_CERTIFICATE") boolean sslAcceptAnyCertificate,
            @Autowired(required = false) @Qualifier("sslCertificateContent") String sslCertificateContent,
            @Autowired(required = false) @Qualifier(BeanIdentifiers.KEY_STORE) String clientKeyStore,
            @Autowired(required = false) @Qualifier(BeanIdentifiers.KEY_STORE_PASSWORD) String clientKeyStorePassword,
            @Autowired(required = false) @Qualifier(BeanIdentifiers.KEY_STORE_PASSWORD_FILE) String clientKeyStorePasswordFile) {

        String keyStorePassword = Optional.ofNullable(clientKeyStorePasswordFile)
                .map(passwordFile -> PasswordFileReader.readPasswordFromFile(nova, passwordFile))
                .orElse(clientKeyStorePassword);


        return HttpClientSettings.builder()
                .compressionEnforced(compressionEnforced)
                .connectionTimeoutInSeconds(connectionTimeoutInSeconds)
                .defaultRequestTimeoutInSeconds(defaultRequestTimeoutInSeconds)
                .webSocketTimeoutInSeconds(webSocketTimeoutInSeconds)
                .sslAcceptAnyCertificate(sslAcceptAnyCertificate)
                .sslKeyStorePath(clientKeyStore)
                .sslKeyStorePass(keyStorePassword)
                .sslCertificateContent(sslCertificateContent)
                .build();
    }

    @Bean(BeanIdentifiers.WEB_SOCKET_TIMEOUT_IN_SECONDS)
    int webSocketTimeoutInSeconds(Environment environment) {
        return environment.getProperty(BeanIdentifiers.WEB_SOCKET_TIMEOUT_IN_SECONDS, int.class, 0);
    }

    @Bean(BeanIdentifiers.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS)
    int defaultRequestTimeoutInSeconds(Environment environment) {
        return environment.getProperty(BeanIdentifiers.DEFAULT_REQUEST_TIMEOUT_IN_SECONDS, int.class, 30);
    }

    @Bean(BeanIdentifiers.CONNECTION_TIMEOUT_IN_SECONDS)
    int connectionTimeoutInSeconds(Environment environment) {
        return environment.getProperty(BeanIdentifiers.CONNECTION_TIMEOUT_IN_SECONDS, int.class, 5);
    }

    @Bean(BeanIdentifiers.COMPRESSION_ENFORCED)
    boolean compressionEnforced(Environment environment) {
        return environment.getProperty(BeanIdentifiers.COMPRESSION_ENFORCED, boolean.class, false);
    }

    @Bean("NOVA.HTTP.CLIENT.ACCEPT_ANY_CERTIFICATE") // No constant to make it a "hidden" feature
    boolean acceptAnyCertificate(Environment environment) {
        return environment.getProperty("NOVA.HTTP.CLIENT.ACCEPT_ANY_CERTIFICATE", boolean.class, false);
    }

    @Bean(BeanIdentifiers.KEY_STORE)
    String clientKeyStore(Environment environment) {
        return environment.getProperty(BeanIdentifiers.KEY_STORE);
    }

    @Bean(BeanIdentifiers.KEY_STORE_PASSWORD)
    String clientKeyStorePassword(Environment environment) {
        return environment.getProperty(BeanIdentifiers.KEY_STORE_PASSWORD);
    }

    @Bean(BeanIdentifiers.KEY_STORE_PASSWORD_FILE)
    String clientKeyStorePasswordFile(Environment environment) {
        return environment.getProperty(BeanIdentifiers.KEY_STORE_PASSWORD_FILE);
    }

    @Bean(BeanIdentifiers.CERTIFICATE)
    String sslCertificate(Environment environment) {
        return environment.getProperty(BeanIdentifiers.CERTIFICATE);
    }

    @Bean("sslCertificateContent")
    String sslCertificateContent (@Autowired(required = false) @Qualifier(BeanIdentifiers.CERTIFICATE) String sslCertificatePath) {
        if (sslCertificatePath==null) {
            return null;
        }

        try {
            byte[] content = Files.readAllBytes(new File(sslCertificatePath).toPath());
            return new String(content, Charset.defaultCharset());
        } catch (Exception error) {
            throw new RuntimeException("Failed to read content of certificate " + sslCertificatePath, error);
        }
    }

    @Bean(BeanIdentifiers.AUTO_CREATE_CLIENT)
    public boolean autoCreateHttpClient(Environment environment) {
        return environment.getProperty(BeanIdentifiers.AUTO_CREATE_CLIENT, Boolean.class, true);
    }

    @Bean(BeanIdentifiers.CLIENT)
    AsyncHttpClient httpClient(@Qualifier(BeanIdentifiers.AUTO_CREATE_CLIENT) boolean autoCreateHttpServer,
                               @Qualifier(BeanIdentifiers.SETTINGS) HttpClientSettings httpClientSettings) {
        if (autoCreateHttpServer) {
            return AsyncHttpClientFactory.clientFor(AsyncHttpClientFactory.builderFor(httpClientSettings));
        } else {
            return null;
        }
    }

    @Bean("autoNotifyAboutHttpClientAvailability")
    public boolean autoNotifyAboutHttpClientAvailability() {
        return true;
    }

    @Bean(BeanIdentifiers.CLIENT_NOTIFIER)
    public HttpClientBeanNotifier httpClientBeanNotifier(@Qualifier("autoNotifyAboutHttpClientAvailability") boolean autoNotifyAboutHttpClientAvailability) {
        return new HttpClientBeanNotifier(autoNotifyAboutHttpClientAvailability);
    }
}