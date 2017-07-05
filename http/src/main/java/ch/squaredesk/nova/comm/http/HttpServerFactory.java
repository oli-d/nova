package ch.squaredesk.nova.comm.http;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Objects;

public class HttpServerFactory {
    public static HttpServer serverFor (HttpServerConfiguration serverConfig) {
        Objects.requireNonNull(serverConfig, "server config must not be null");
        return HttpServer.createSimpleServer(null, serverConfig.interfaceName, serverConfig.port);
    }
}
