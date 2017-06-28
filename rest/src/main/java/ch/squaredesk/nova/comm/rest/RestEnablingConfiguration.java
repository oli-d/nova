/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.nova.Nova;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@Configuration
public abstract class RestEnablingConfiguration {
    @Autowired
    Environment environment;

    @Autowired
    Nova nova;

    @Bean
    public RestBeanPostprocessor getBeanPostProcessor() {
        return new RestBeanPostprocessor(resourceConfig());
    }

    @Bean
    public ResourceConfig resourceConfig() {
        return new ResourceConfig();
    }

    @Bean
    public int restPort() {
        return environment.getProperty("NOVA.HTTP.REST.PORT", Integer.class, 8888);
    }

    @Bean
    public String restInterfaceName() {
        String interfaceName = environment.getProperty("NOVA.HTTP.INTERFACE_NAME", "");
        if ("".equals(interfaceName)) {
            interfaceName = "localhost";
        }
        return interfaceName;
    }


    @Bean
    @Lazy // must be created after all other beans have been created (because of annotation processing)
    public HttpServer restHttpServer() {
        URI serverAddress = UriBuilder.fromPath("http://" + restInterfaceName() + ":" + restPort()).build();
        return GrizzlyHttpServerFactory.createHttpServer(serverAddress, resourceConfig());
    }
}
