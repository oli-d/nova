/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.core;


import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.autoconfigure.core.events.EventHandlingBeanPostprocessor;
import ch.squaredesk.nova.autoconfigure.service.NovaServiceConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Nova.class)
@EnableConfigurationProperties({NovaConfigurationProperties.class, EventDispatchConfigurationProperties.class})
public class NovaAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Nova nova(NovaConfigurationProperties novaConfigurationProperties,
                     EventDispatchConfigurationProperties eventDispatchConfigurationProperties) {
        return Nova.builder()
                .setIdentifier(novaConfigurationProperties.getIdentifier())
                .captureJvmMetrics(novaConfigurationProperties.isCaptureJvmMetrics())
                .setWarnOnUnhandledEvents(eventDispatchConfigurationProperties.getWarnOnUnhandledEvent())
                .setDefaultBackpressureStrategy(eventDispatchConfigurationProperties.getBackpressureStrategy())
                .setEventDispatchMode(eventDispatchConfigurationProperties.getEventDispatchMode())
                .setParallelism(eventDispatchConfigurationProperties.getParallelism())
                .build();
    }

    @Bean
    @ConditionalOnBean(Nova.class)
    public EventHandlingBeanPostprocessor eventHandlingBeanPostProcessor(Nova nova) {
        return new EventHandlingBeanPostprocessor(nova);
    }
}
