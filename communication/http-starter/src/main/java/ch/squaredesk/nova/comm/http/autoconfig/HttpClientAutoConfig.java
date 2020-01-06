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
@ConditionalOnProperty(name = "nova.http.client.enable", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(HttpClientConfigurationProperties.class)
public class HttpClientAutoConfig {

    @Bean(BeanIdentifiers.CLIENT)
    @ConditionalOnMissingBean(name = BeanIdentifiers.CLIENT)
    AsyncHttpClient httpClient(HttpClientConfigurationProperties httpClientConfigurationProperties) {
        String sslCertificateContent = httpClientConfigurationProperties.getSslCertificateContent();
        if (sslCertificateContent == null || sslCertificateContent.trim().isEmpty()) {
            sslCertificateContent = Optional.ofNullable(httpClientConfigurationProperties.getSslCertificatePath())
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
                .compressionEnforced(httpClientConfigurationProperties.isCompressionEnforced())
                .connectionTimeoutInSeconds(httpClientConfigurationProperties.getConnectionTimeoutInSeconds())
                .defaultRequestTimeoutInSeconds(httpClientConfigurationProperties.getDefaultRequestTimeoutInSeconds())
                .sslAcceptAnyCertificate(httpClientConfigurationProperties.isAcceptAnyCertificate())
                .sslCertificateContent(sslCertificateContent)
                .sslKeyStorePass(httpClientConfigurationProperties.getSslKeyStorePass())
                .sslKeyStorePath(httpClientConfigurationProperties.getSslKeyStorePath())
                .userAgent(httpClientConfigurationProperties.getUserAgent())
                .webSocketTimeoutInSeconds(httpClientConfigurationProperties.getWebSocketTimeoutInSeconds())
                .build();

        return AsyncHttpClientFactory.clientFor(settings);
    }
}