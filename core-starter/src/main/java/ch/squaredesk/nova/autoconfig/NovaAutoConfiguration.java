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
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Nova.class)
@EnableConfigurationProperties({NovaConfigurationProperties.class, NovaServiceConfigurationProperties.class, EventDispatchConfigurationProperties.class})
public class NovaAutoConfiguration {
    @Bean("NOVA.INSTANCE")
    @ConditionalOnMissingBean(name = "NOVA.INSTANCE")
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

    @Bean("novaServiceEnhancementsFlag")
    @ConditionalOnBean(name = "NOVA.INSTANCE")
    public Boolean novaServiceEnhancementsFlag(
            ApplicationContext applicationContext,
            NovaServiceConfigurationProperties novaServiceConfigurationProperties) {

        Object springBootApplication = applicationContext.getBeansWithAnnotation(SpringBootApplication.class);

        return true;
    }

    @Bean
    public EventHandlingBeanPostprocessor eventHandlingBeanPostProcessor() {
        return new EventHandlingBeanPostprocessor();
    }
}
