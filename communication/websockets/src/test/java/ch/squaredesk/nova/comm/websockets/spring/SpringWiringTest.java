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
import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import ch.squaredesk.nova.comm.websockets.WebSocketAdapter;
import ch.squaredesk.nova.comm.websockets.spring.annotation.OnClose;
import ch.squaredesk.nova.comm.websockets.spring.annotation.OnConnect;
import ch.squaredesk.nova.comm.websockets.spring.annotation.OnMessage;
import ch.squaredesk.nova.tuples.Pair;
import org.awaitility.Awaitility;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
    @Autowired
    MyBean myBean;

    @Test
    public void webSocketEndpointCanProperlyBeInvoked() throws Exception {
        // verify all clean
        httpServer.start();
        assertThat(myBean.connectedSockets.isEmpty(), is(true));
        assertThat(myBean.closedSockets.isEmpty(), is(true));

        // connect two sockets
        String serverUrl = "ws://127.0.0.1:" + httpServerSettings.port;
        WebSocket clientSideSocket1 = webSocketAdapter.connectTo(serverUrl+"/echo");
        WebSocket clientSideSocket2 = webSocketAdapter.connectTo(serverUrl+"/echo");

        // verify connect handlers invoked
        assertThat(myBean.connectedSockets.size(), is(2));
        assertThat(myBean.closedSockets.isEmpty(), is(true));

        // send a message on each client socket
        CountDownLatch cdl = new CountDownLatch(2);
        Integer[] resultHolder = new Integer[2];
        clientSideSocket1.messages(Integer.class).subscribe(msg -> {
            resultHolder[0] = msg.message;
            cdl.countDown();
        });
        clientSideSocket2.messages(Integer.class).subscribe(msg -> {
            resultHolder[1] = msg.message;
            cdl.countDown();
        });

        Integer dataToSend1 = new Random().nextInt();
        clientSideSocket1.send(dataToSend1);
        Integer dataToSend2 = new Random().nextInt();
        clientSideSocket2.send(dataToSend2);

        // verify message handlers invoked
        cdl.await(5, TimeUnit.SECONDS);
        assertThat(cdl.getCount(), is(0L));
        assertThat(resultHolder[0], is(dataToSend1));
        assertThat(resultHolder[1], is(dataToSend2));

        // close one socket
        clientSideSocket1.close();

        // verify connect handlers invoked
        assertThat(myBean.connectedSockets.size(), is(2));
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(myBean.closedSockets::size, is(1));

        assertThat(nova.metrics.getMeter("websocket", "received", "echo").getCount(), is(2L));
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
        private final List<WebSocket> connectedSockets = new ArrayList<>();
        private final List<Pair<WebSocket, CloseReason>> closedSockets = new ArrayList<>();

        @OnMessage("echo")
        public void websocketEchoInteger(Integer message, WebSocket webSocket) throws Exception {
            webSocket.send(message);
        }

        @OnConnect("echo")
        public void websocketConnectHandler(WebSocket webSocket) throws Exception {
            connectedSockets.add(webSocket);
        }

        @OnClose("echo")
        public void websocketClosedHandler(WebSocket webSocket, CloseReason closeReason) throws Exception {
            closedSockets.add(new Pair<>(webSocket, closeReason));
        }
    }
}