/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.service;


import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.metrics.MetricsDump;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Configuration
@ConditionalOnClass({Nova.class, NovaService.class})
@EnableConfigurationProperties({NovaServiceConfigurationProperties.class})
public class NovaServiceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(NovaService.class)
    ServiceDescriptor novaServiceConfig(NovaServiceConfigurationProperties props, NovaService theServiceInstance) {
        String serviceName = Optional.ofNullable(props.getServiceName())
                .orElse(calculateDefaultServiceName(theServiceInstance));
        String instanceId = Optional.ofNullable(props.getInstanceId())
                .orElse(UUID.randomUUID().toString());

        return new ServiceDescriptor(serviceName, instanceId);
    }

    String calculateDefaultServiceName(Object serviceInstance) {
        String simpleClassName = serviceInstance.getClass().getSimpleName();
        int indexOfDollor = simpleClassName.indexOf('$');
        if (indexOfDollor > 0) {
            return simpleClassName.substring(0, indexOfDollor);
        } else {
            return simpleClassName;
        }
    }

    @Bean
    @ConditionalOnBean(Nova.class)
    MetricsInitializer metricsInitializer() {
        return new MetricsInitializer();
    }
}
