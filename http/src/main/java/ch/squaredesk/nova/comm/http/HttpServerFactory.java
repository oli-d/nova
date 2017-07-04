package ch.squaredesk.nova.comm.http;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Objects;

public class HttpServerFactory {
    public static HttpServer serverFor (HttpServerConfiguration serverConfig /*, ResourceConfig resourceConfig*/) {
        Objects.requireNonNull(serverConfig, "server config must not be null");

        // URI serverAddress = UriBuilder.fromPath("http://" + serverConfig.interfaceName + ":" + serverConfig.port).build();
        // return GrizzlyHttpServerFactory.createHttpServer(serverAddress, resourceConfig, false);
        return HttpServer.createSimpleServer(null, serverConfig.interfaceName, serverConfig.port);
    }
}
