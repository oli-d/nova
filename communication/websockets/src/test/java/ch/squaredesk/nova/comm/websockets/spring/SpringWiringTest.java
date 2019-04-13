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
package ch.squaredesk.nova.comm.websockets.spring;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpServerSettings;
import ch.squaredesk.nova.comm.http.spring.HttpServerProvidingConfiguration;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import ch.squaredesk.nova.comm.websockets.WebSocketAdapter;
import ch.squaredesk.nova.comm.websockets.annotation.OnMessage;
import ch.squaredesk.nova.comm.websockets.client.ClientEndpoint;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@Tag("medium")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { WebSocketEnablingConfiguration.class, SpringWiringTest.MyConfig.class})
public class SpringWiringTest {
    @Autowired
    HttpServerSettings httpServerSettings;
    @Autowired
    HttpServer httpServer;
    @Autowired
    Nova nova;
    @Autowired
    WebSocketAdapter webSocketAdapter;

    @Test
    public void webSocketEndpointCanProperlyBeInvoked() throws Exception {
        httpServer.start();

        String serverUrl = "ws://127.0.0.1:" + httpServerSettings.port;
        ClientEndpoint clientSideSocket = webSocketAdapter.connectTo(serverUrl+"/echo");
        CountDownLatch cdl = new CountDownLatch(1);
        Integer[] resultHolder = new Integer[1];
        clientSideSocket.messages(Integer.class).subscribe(msg -> {
            resultHolder[0] = msg.message;
            cdl.countDown();
        });
        Integer dataToSend = new Random().nextInt();
        clientSideSocket.send(dataToSend);

        cdl.await(5, TimeUnit.SECONDS);
        assertThat(cdl.getCount(), is (0L));
        assertThat(resultHolder[0], is(dataToSend));

        assertThat(nova.metrics.getMeter("websocket", "received", "echo").getCount(), is(1L));
    }

    @Configuration
    public static class MyConfig  {
        @Bean
        public MyBean myBean() {
            return new MyBean();
        }

        @Bean(HttpServerProvidingConfiguration.BeanIdentifiers.PORT)
        public int serverPort() {
            return PortFinder.findFreePort();
        }
    }

    public static class MyBean {
        @OnMessage("echo")
        public void websocketEchoInteger(Integer message, WebSocket webSocket) throws Exception {
            webSocket.send(message);
        }
    }
}