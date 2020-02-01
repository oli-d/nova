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
package ch.squaredesk.nova.comm.http;

import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpServerStarter implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {
    private final HttpServer httpServer;

    public HttpServerStarter(HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws IOException {
        if (!httpServer.isStarted()) {
            httpServer.start();
        }
    }

    @Override
    public void destroy() {
        try {
            httpServer.shutdown().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            // noop; shutdown anyway...
        }
    }
}
