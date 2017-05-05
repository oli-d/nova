/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service;


import ch.squaredesk.nova.eventannotations.AnnotationEnablingConfiguration;
import ch.squaredesk.nova.eventannotations.NovaProvidingConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Configuration
@Import(AnnotationEnablingConfiguration.class)
public abstract class NovaServiceConfiguration<ServiceType> extends NovaProvidingConfiguration {
    @Autowired
    protected Environment environment;

    @Bean(name = "instanceId")
    public String getInstanceId() {
        return UUID.randomUUID().toString();
    }

    @Bean(name = "serviceName")
    public String serviceName() {
        String configClassName = getClass().getSimpleName();
        Optional<Integer> indexOfConfigSubstring = Stream.of("config", "conf", "cfg")
                .map(testString -> configClassName.toLowerCase().indexOf(testString))
                .max(Integer::compareTo);
        return configClassName.substring(0,indexOfConfigSubstring.orElse(configClassName.length()));
    }

    public abstract ServiceType createServiceInstance();
}
