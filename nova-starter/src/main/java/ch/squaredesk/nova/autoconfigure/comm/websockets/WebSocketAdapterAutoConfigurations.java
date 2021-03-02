/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.websockets;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.autoconfigure.core.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.MessageTranscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter({NovaAutoConfiguration.class})
@EnableConfigurationProperties(WebSocketAdapterConfigurationProperties.class)
// @ConditionalOnClass(WebSocketAdapter.class)
public class WebSocketAdapterAutoConfigurations {
//    @Bean
//    @ConditionalOnMissingBean(WebSocketAdapter.class)
//    WebSocketAdapter webSocketAdapter(
//            WebSocketAdapterConfigurationProperties webSocketAdapterConfigurationProperties,
//            @Qualifier(BeanIdentifiers.MESSAGE_TRANSCRIBER) MessageTranscriber<String> webSocketMessageTranscriber,
//            @Qualifier(BeanIdentifiers.SERVER) @Autowired(required = false) HttpServer httpServer,
//            Nova nova) {
//        return WebSocketAdapter.builder()
//                .setIdentifier(webSocketAdapterConfigurationProperties.getAdapterIdentifier())
//                .setHttpServer(httpServer)
//                .setMessageTranscriber(webSocketMessageTranscriber)
//                .setMetrics(nova.metrics)
//                .build();
//    }
//
//    @Bean
//    @ConditionalOnBean(WebSocketAdapter.class)
//    @ConditionalOnMissingBean(WebSocketBeanProcessor.class)
//    WebSocketBeanProcessor webSocketBeanPostprocessor(
//            WebSocketAdapter webSocketAdapter,
//            WebSocketAdapterConfigurationProperties webSocketAdapterConfigurationProperties,
//            @Qualifier(BeanIdentifiers.MESSAGE_TRANSCRIBER) MessageTranscriber<String> webSocketMessageTranscriber,
//            Nova nova) {
//        return new WebSocketBeanProcessor(
//                webSocketAdapter,
//                webSocketMessageTranscriber,
//                webSocketAdapterConfigurationProperties.getAdapterIdentifier(),
//                nova.metrics);
//    }
}
