/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http.annotation;

import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RestEnablingConfiguration {
    @Autowired
    Environment environment;

    @Bean
    public RestBeanPostprocessor getBeanPostProcessor() {
        return new RestBeanPostprocessor(resourceConfig());
    }

    @Bean
    public ResourceConfig resourceConfig() {
        return new ResourceConfig();
    }

    @Bean
    public HttpServerConfiguration restServerConfiguration() {
        int restPort = environment.getProperty("NOVA.HTTP.REST.PORT", Integer.class, 10000);
        String interfaceName = environment.getProperty("NOVA.HTTP.REST.INTERFACE_NAME", "0.0.0.0");
        return new HttpServerConfiguration(
            interfaceName,
            restPort
        );
    }
}
