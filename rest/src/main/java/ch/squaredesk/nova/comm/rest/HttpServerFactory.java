package ch.squaredesk.nova.comm.rest;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Objects;

public class HttpServerFactory {
    public static HttpServer serverFor (RestServerConfiguration serverConfig, ResourceConfig resourceConfig) {
        Objects.requireNonNull(serverConfig, "server config must not be null");

        if (resourceConfig == null || resourceConfig.getResources().isEmpty()) {
            throw new IllegalArgumentException("Not creating REST server without resources");
        }

        URI serverAddress = UriBuilder.fromPath("http://" + serverConfig.interfaceName + ":" + serverConfig.port).build();
        return GrizzlyHttpServerFactory.createHttpServer(serverAddress, resourceConfig);
    }
}
