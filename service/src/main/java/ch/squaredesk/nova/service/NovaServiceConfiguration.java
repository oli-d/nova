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

package ch.squaredesk.nova.service;


import ch.squaredesk.nova.events.annotation.AnnotationEnablingConfiguration;
import ch.squaredesk.nova.service.annotation.LifecycleBeanProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.UUID;

@Configuration
public class NovaServiceConfiguration  {
    public interface BeanIdentifiers {
        String INSTANCE  = "NOVA.SERVICE.INSTANCE";
        String INSTANCE_IDENTIFIER = "NOVA.SERVICE.INSTANCE_IDENTIFIER";
        String NAME = "NOVA.SERVICE.NAME";
        String REGISTER_SHUTDOWN_HOOK = "NOVA.SERVICE.REGISTER_SHUTDOWN_HOOK";
        String CONFIG_FILE = "NOVA.SERVICE.CONFIG_FILE";
    }
    @Autowired
    protected Environment environment;

    @Bean(BeanIdentifiers.INSTANCE_IDENTIFIER)
    public String instanceId() {
        return environment.getProperty(BeanIdentifiers.INSTANCE_IDENTIFIER, UUID.randomUUID().toString());
    }

    @Bean(BeanIdentifiers.NAME)
    public String serviceName() {
        return environment.getProperty(BeanIdentifiers.NAME);
    }

    @Bean
    public static LifecycleBeanProcessor lifecycleBeanProcessor() {
        return new LifecycleBeanProcessor();
    }

    @Bean(BeanIdentifiers.REGISTER_SHUTDOWN_HOOK)
    public boolean registerShutdownHook() {
        boolean registerShutdownHook = true;
        try {
            registerShutdownHook = Boolean.parseBoolean(
                    environment.getProperty(BeanIdentifiers.REGISTER_SHUTDOWN_HOOK, "TRUE"));
        } catch (Exception e) {
            // noop, stick with default
        }
        return registerShutdownHook;
    }
}
