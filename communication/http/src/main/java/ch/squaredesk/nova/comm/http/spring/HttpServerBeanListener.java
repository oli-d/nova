package ch.squaredesk.nova.comm.http.spring;

import org.glassfish.grizzly.http.server.HttpServer;

public interface HttpServerBeanListener {
    void httpServerAvailableInContext(HttpServer httpServer);
}
