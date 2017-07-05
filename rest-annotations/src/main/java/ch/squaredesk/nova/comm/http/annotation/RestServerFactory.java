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
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class RestServerFactory {
    public static HttpServer serverFor(HttpServerConfiguration serverConfig, ResourceConfig resourceConfig) {
        URI serverAddress = UriBuilder.fromPath("http://" + serverConfig.interfaceName + ":" + serverConfig.port).build();
        return GrizzlyHttpServerFactory.createHttpServer(serverAddress, resourceConfig, true);
    }

}
