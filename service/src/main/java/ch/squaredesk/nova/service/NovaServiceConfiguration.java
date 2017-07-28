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


import ch.squaredesk.nova.events.annotation.AnnotationEnablingConfiguration;
import ch.squaredesk.nova.events.annotation.NovaProvidingConfiguration;
import ch.squaredesk.nova.service.annotation.LifecycleBeanProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Configuration
@PropertySource(value="classpath:defaults.properties", ignoreResourceNotFound = true)
@PropertySource(value="file:${NOVA.SERVICE.CONFIG}", ignoreResourceNotFound = true)
@PropertySource(value="classpath:${NOVA.SERVICE.CONFIG}", ignoreResourceNotFound = true)
@Import({AnnotationEnablingConfiguration.class, NovaProvidingConfiguration.class})
public abstract class NovaServiceConfiguration<ServiceType>  {
    @Autowired
    protected Environment environment;

    @Bean(name = "instanceId")
    public String instanceId() {
        return environment.getProperty("NOVA.SERVICE.INSTANCE_ID", UUID.randomUUID().toString());
    }

    @Bean(name = "serviceName")
    public String serviceName() {
        String name = environment.getProperty("NOVA.SERVICE.NAME", (String)null);
        if (name == null) {
            String configClassName = getClass().getSimpleName();
            Optional<Integer> indexOfConfigSubstring = Stream.of("config", "conf", "cfg")
                    .map(testString -> configClassName.toLowerCase().indexOf(testString))
                    .max(Integer::compareTo);
            name = configClassName.substring(0, indexOfConfigSubstring.orElse(configClassName.length()));
        }

        return name;
    }

    @Bean
    public static LifecycleBeanProcessor lifecycleBeanProcessor() {
        return new LifecycleBeanProcessor();
    }

    @Bean
    public boolean registerShutdownHook() {
        boolean registerShutdownHook = true;
        try {
            registerShutdownHook = Boolean.parseBoolean(
                    environment.getProperty("NOVA.SERVICE.REGISTER_SHUTDOWN_HOOK", "TRUE"));
        } catch (Exception e) {
            // noop, stick with default
        }
        return registerShutdownHook;
    }

    public abstract ServiceType serviceInstance();
}
