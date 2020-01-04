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

package ch.squaredesk.nova.comm.http.autoconfig;

import ch.squaredesk.nova.comm.http.AsyncHttpClientFactory;
import com.ning.http.client.AsyncHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(HttpClientSettings.class)
@ConditionalOnProperty(name = "nova.http.client.enable", havingValue = "true", matchIfMissing = true)
public class HttpClientAutoConfig {

    @Bean(BeanIdentifiers.CLIENT)
    @ConditionalOnMissingBean(name = BeanIdentifiers.CLIENT)
    AsyncHttpClient httpClient(HttpClientSettings httpClientSettings) {
        String sslCertificateContent = httpClientSettings.getSslCertificateContent();
        if (sslCertificateContent == null || sslCertificateContent.trim().isEmpty()) {
            sslCertificateContent = Optional.ofNullable(httpClientSettings.getSslCertificatePath())
                    .map(certPath -> {
                        try {
                            byte[] content = Files.readAllBytes(new File(certPath).toPath());
                            return new String(content, Charset.defaultCharset());
                        } catch (Exception error) {
                            throw new RuntimeException("Failed to read content of certificate " + certPath, error);
                        }
                    })
                    .orElse(null)
                    ;
        }

        ch.squaredesk.nova.comm.http.HttpClientSettings settings =
                ch.squaredesk.nova.comm.http.HttpClientSettings.builder()
                .compressionEnforced(httpClientSettings.isCompressionEnforced())
                .connectionTimeoutInSeconds(httpClientSettings.getConnectionTimeoutInSeconds())
                .defaultRequestTimeoutInSeconds(httpClientSettings.getDefaultRequestTimeoutInSeconds())
                .sslAcceptAnyCertificate(httpClientSettings.isAcceptAnyCertificate())
                .sslCertificateContent(sslCertificateContent)
                .sslKeyStorePass(httpClientSettings.getSslKeyStorePass())
                .sslKeyStorePath(httpClientSettings.getSslKeyStorePath())
                .userAgent(httpClientSettings.getUserAgent())
                .webSocketTimeoutInSeconds(httpClientSettings.getWebSocketTimeoutInSeconds())
                .build();

        return AsyncHttpClientFactory.clientFor(settings);
    }
}