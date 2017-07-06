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
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

@Configuration
@Import(RestEnablingConfiguration.class)
public class RestServerProvidingConfiguration {
    @Autowired
    ResourceConfig resourceConfig;

    @Autowired
    HttpServerConfiguration serverConfig;

    @Bean
    RestServerStarter restServerStarter() {
        return new RestServerStarter();
    }

    @Lazy // must be created after all other beans have been created (because of annotation processing)
    @Bean
    public HttpServer restHttpServer() {
        return RestServerFactory.serverFor(serverConfig, resourceConfig);
    }

}
