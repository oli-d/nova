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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.PreDestroy;
import java.io.IOException;

public class HttpServerStarter implements ApplicationListener<ContextRefreshedEvent> {
    private Logger logger = LoggerFactory.getLogger(HttpServerStarter.class);

    private HttpServer httpServer;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        httpServer = event.getApplicationContext().getBean("httpServer", HttpServer.class);

        if (!httpServer.isStarted()) {
            try {
                httpServer.start();
            } catch (IOException e) {
                logger.error("Unable to start HttpServer", e);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        if (httpServer != null) {
            httpServer.shutdown();
        }
    }

}
