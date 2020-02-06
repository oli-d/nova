/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.http;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.autoconfigure.core.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.http.HttpAdapter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.AsyncHttpClient;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@AutoConfigureAfter({NovaAutoConfiguration.class, HttpClientAutoConfiguration.class, HttpServerAutoConfiguration.class})
@EnableConfigurationProperties(HttpAdapterConfigurationProperties.class)
public class HttpAdapterAutoConfiguration {
    @Bean(BeanIdentifiers.OBJECT_MAPPER)
    @ConditionalOnMissingBean(name = BeanIdentifiers.OBJECT_MAPPER)
    @ConditionalOnClass(ObjectMapper.class)
    ObjectMapper httpObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndRegisterModules()
                ;
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    @ConditionalOnMissingBean(MessageTranscriber.class)
    @ConditionalOnBean(name = BeanIdentifiers.OBJECT_MAPPER)
    MessageTranscriber<String> httpMessageTranscriberWithJackson(@Qualifier(BeanIdentifiers.OBJECT_MAPPER) ObjectMapper jmsObjectMapper) {
        return new DefaultMessageTranscriberForStringAsTransportType(jmsObjectMapper);
    }

    @Bean(BeanIdentifiers.MESSAGE_TRANSCRIBER)
    @ConditionalOnMissingBean(name = {BeanIdentifiers.MESSAGE_TRANSCRIBER, BeanIdentifiers.OBJECT_MAPPER})
    MessageTranscriber<String> httpMessageTranscriberWithoutJackson() {
        return new DefaultMessageTranscriberForStringAsTransportType();
    }

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
                .setDefaultRequestTimeout(Duration.ofSeconds(clientSettings.getDefaultRequestTimeoutInSeconds()))
                .setHttpClient(httpClient)
                .setHttpServer(httpServer)
                .setIdentifier(adapterSettings.getAdapterIdentifier())
                .setMessageTranscriber(httpMessageTranscriber)
                .setMetrics(nova.metrics)
                .build();
    }
}
