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

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.autoconfig.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.HttpAdapter;
import com.ning.http.client.AsyncHttpClient;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@ImportAutoConfiguration({HttpServerAutoConfig.class, HttpClientAutoConfig.class, HttpAdapterMessageTranscriberAutoConfig.class})
@AutoConfigureAfter(NovaAutoConfiguration.class)
@EnableConfigurationProperties(HttpAdapterConfigurationProperties.class)
public class HttpAdapterAutoConfig {
    @Bean
    @ConditionalOnMissingBean(HttpAdapter.class)
    public HttpAdapter httpAdapter(
            HttpAdapterConfigurationProperties adapterSettings,
            HttpClientConfigurationProperties clientSettings,
            @Qualifier(BeanIdentifiers.CLIENT) @Autowired(required = false) AsyncHttpClient httpClient,
            @Qualifier(BeanIdentifiers.SERVER) @Autowired(required = false) HttpServer httpServer,
            @Qualifier(BeanIdentifiers.MESSAGE_TRANSCRIBER) MessageTranscriber<String> httpMessageTranscriber,
            Nova nova) {
        return HttpAdapter.builder()
                .setDefaultRequestTimeout(clientSettings.getDefaultRequestTimeoutInSeconds(), TimeUnit.SECONDS)
                .setHttpClient(httpClient)
                .setHttpServer(httpServer)
                .setIdentifier(adapterSettings.getAdapterIdentifier())
                .setMessageTranscriber(httpMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }
}
