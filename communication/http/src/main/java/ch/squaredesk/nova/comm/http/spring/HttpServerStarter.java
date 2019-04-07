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

import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.PreDestroy;
import java.io.IOException;

public class HttpServerStarter implements ApplicationListener<ContextRefreshedEvent> {
    private final HttpServer httpServer;
    private final boolean autoStartServerWhenApplicationContextRefreshed;

    public HttpServerStarter(HttpServer httpServer, boolean autoStartServerWhenApplicationContextRefreshed) {
        this.httpServer = httpServer;
        this.autoStartServerWhenApplicationContextRefreshed = autoStartServerWhenApplicationContextRefreshed;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (httpServer!= null && autoStartServerWhenApplicationContextRefreshed) {
            try {
                start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void start() throws IOException {
        if (!httpServer.isStarted()) {
            httpServer.start();
        }
    }

    @PreDestroy
    public void shutdown() {
        if (httpServer != null) {
            httpServer.shutdown();
        }
    }


}
