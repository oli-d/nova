/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.net.PortFinder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Tag("medium")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HttpServerBeanListenerTest.Config.class})
class HttpServerBeanListenerTest {
    @Autowired
    private HttpServer server;
    @Autowired
    private MyListener listener;


    @Test
    void listenerGetsInvokedWhenHttpServerIsAvailable() throws Exception {
        MatcherAssert.assertThat(listener.httpServer, Matchers.sameInstance(server));
    }

    @Configuration
    @Import(HttpEnablingConfiguration.class)
    public static class Config {
        @Bean(HttpServerProvidingConfiguration.BeanIdentifiers.PORT)
        public Integer httpServerPort() {
            return PortFinder.findFreePort();
        }

        @Bean
        public MyListener myListener() {
            return new MyListener();
        }
    }

    public static class MyListener implements HttpServerBeanListener {
        private HttpServer httpServer;

        @Override
        public void httpServerAvailableInContext(HttpServer httpServer) {
            this.httpServer = httpServer;
        }
    }
}