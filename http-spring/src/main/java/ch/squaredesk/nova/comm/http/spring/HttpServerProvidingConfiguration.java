/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import ch.squaredesk.nova.comm.http.HttpServerFactory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(HttpServerConfigurationProvidingConfiguration.class)
public class HttpServerProvidingConfiguration {
    @Autowired
    HttpServerConfiguration httpServerConfiguration;

    @Bean("httpServer")
    public HttpServer httpServer() {
        return HttpServerFactory.serverFor(httpServerConfiguration);
    }
}
