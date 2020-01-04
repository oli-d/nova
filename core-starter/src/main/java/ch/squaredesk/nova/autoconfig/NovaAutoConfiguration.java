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

package ch.squaredesk.nova.autoconfig;


import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.autoconfig.annotation.EventHandlingBeanPostprocessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Nova.class)
@EnableConfigurationProperties(NovaSettings.class)
public class NovaAutoConfiguration {
    @Bean("NOVA.INSTANCE")
    @ConditionalOnMissingBean(name = "NOVA.INSTANCE")
    public Nova nova(NovaSettings settings) {
        return Nova.builder()
                .setIdentifier(settings.getIdentifier())
                .setDefaultBackpressureStrategy(settings.getDefaultBackpressureStrategy())
                .setWarnOnUnhandledEvent(settings.getWarnOnUnhandledEvent())
                .captureJvmMetrics(settings.getCaptureJvmMetrics())
                .build();
    }

    @Bean
    public EventHandlingBeanPostprocessor eventHandlingBeanPostProcessor() {
        return new EventHandlingBeanPostprocessor();
    }
}
