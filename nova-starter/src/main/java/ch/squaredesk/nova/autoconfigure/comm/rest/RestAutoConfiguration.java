/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.rest;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.autoconfigure.comm.http.*;
import ch.squaredesk.nova.autoconfigure.core.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.http.HttpAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.AsyncHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(NovaAutoConfiguration.class)
@EnableConfigurationProperties({
        HttpServerConfigurationProperties.class,
        HttpClientConfigurationProperties.class,
        RestConfigurationProperties.class})
@ConditionalOnClass({HttpAdapter.class, AsyncHttpClient.class})
@AutoConfigureBefore({HttpServerAutoConfiguration.class, HttpClientAutoConfiguration.class})
public class RestAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "nova.http.rest.log-invocations", havingValue = "true", matchIfMissing = true)
    RestInvocationLogger restInvocationLogger() {
        return new RestInvocationLogger();
    }

    @Bean
    @ConditionalOnMissingBean(name = BeanIdentifiers.SERVER)
    RestServerStarter restServerStarter(HttpServerConfigurationProperties httpServerConfigurationProperties,
                                        RestConfigurationProperties restConfigurationProperties,
                                        RestBeanPostprocessor restBeanPostprocessor,
                                        @Qualifier(BeanIdentifiers.OBJECT_MAPPER) @Autowired(required = false) ObjectMapper httpObjectMapper,
                                        Nova nova) {

        ch.squaredesk.nova.comm.http.HttpServerSettings serverSettings =
                ch.squaredesk.nova.comm.http.HttpServerSettings.builder()
                        .port(httpServerConfigurationProperties.getPort())
                        .compressData(httpServerConfigurationProperties.isCompressData())
                        .sslNeedsClientAuth(httpServerConfigurationProperties.isSslNeedsClientAuth())
                        .sslKeyStorePass(httpServerConfigurationProperties.getSslKeyStorePass())
                        .sslKeyStorePath(httpServerConfigurationProperties.getSslKeyStorePath())
                        .sslTrustStorePass(httpServerConfigurationProperties.getSslTrustStorePass())
                        .sslTrustStorePath(httpServerConfigurationProperties.getSslTrustStorePath())
                        .interfaceName(httpServerConfigurationProperties.getInterfaceName())
                        .addCompressibleMimeTypes(httpServerConfigurationProperties.getCompressibleMimeTypes().toArray(new String[0]))
                        .build();

        return new RestServerStarter(
                serverSettings,
                restConfigurationProperties.getServerProperties(),
                restBeanPostprocessor,
                httpObjectMapper,
                restConfigurationProperties.isCaptureMetrics(),
                nova);
    }

    @Bean
    @ConditionalOnMissingBean(name = BeanIdentifiers.CLIENT)
    RestClientStarter restClientStarter(HttpClientConfigurationProperties httpClientConfigurationProperties) {

        ch.squaredesk.nova.comm.http.HttpClientSettings clientSettings =
                ch.squaredesk.nova.comm.http.HttpClientSettings.builder()
                        .compressionEnforced(httpClientConfigurationProperties.isCompressionEnforced())
                        .connectionTimeoutInSeconds(httpClientConfigurationProperties.getConnectionTimeoutInSeconds())
                        .defaultRequestTimeoutInSeconds(httpClientConfigurationProperties.getDefaultRequestTimeoutInSeconds())
                        .sslAcceptAnyCertificate(httpClientConfigurationProperties.isAcceptAnyCertificate())
                        .sslCertificateContent(httpClientConfigurationProperties.getSslCertificateContent())
                        .sslKeyStorePass(httpClientConfigurationProperties.getSslKeyStorePass())
                        .sslKeyStorePath(httpClientConfigurationProperties.getSslKeyStorePath())
                        .userAgent(httpClientConfigurationProperties.getUserAgent())
                        .webSocketTimeoutInSeconds(httpClientConfigurationProperties.getWebSocketTimeoutInSeconds())
                        .build();

        return new RestClientStarter(clientSettings);
    }

    @Bean
    RestBeanPostprocessor restBeanPostprocessor() {
        return new RestBeanPostprocessor();
    }

}
