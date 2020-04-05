/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.service;


import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.service.NovaService;
import ch.squaredesk.nova.service.ServiceDescriptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({Nova.class, NovaService.class})
@EnableConfigurationProperties({NovaServiceConfigurationProperties.class})
public class NovaServiceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    ServiceDescriptor novaServiceConfig(NovaServiceConfigurationProperties props) {
        return new ServiceDescriptor(
                props.getServiceName(),
                props.getInstanceId(),
                props.isServiceLifecycleEnabled()
        );
    }

}
