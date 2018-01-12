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
package ch.squaredesk.nova.comm.websockets.annotation;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import ch.squaredesk.nova.comm.websockets.WebSocketAdapter;
import ch.squaredesk.nova.comm.websockets.client.ClientEndpoint;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import com.ning.http.client.AsyncHttpClient;
import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class SpringWiringTest {

    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    private String serverUrl;

    private void setupContext(Class configClass) throws Exception {
        ctx.register(configClass);
        ctx.refresh();
        HttpServerConfiguration cfg = ctx.getBean(HttpServerConfiguration.class);
        serverUrl = "ws://127.0.0.1:" + cfg.port;
    }

    @AfterEach
    public void tearDown() {
        ctx.close();
    }

    @Test
    public void webSocketEndpointCanProperlyBeInvoked() throws Exception {
        setupContext(MyConfig.class);
        Metrics metrics = ctx.getBean(Nova.class).metrics;
        MatcherAssert.assertThat(metrics.getMeter("websocket", "received", "echo").getCount(), is(0L));

        WebSocketAdapter<Integer> webSocketAdapter = ctx.getBean(WebSocketAdapter.class);
        ClientEndpoint<Integer> clientSideSocket = webSocketAdapter.connectTo(serverUrl+"/echo");
        CountDownLatch cdl = new CountDownLatch(1);
        Integer[] resultHolder = new Integer[1];
        clientSideSocket.messages().subscribe(msg -> {
            resultHolder[0] = msg.message;
            cdl.countDown();
        });
        Integer dataToSend = new Random().nextInt();
        clientSideSocket.send(dataToSend);

        cdl.await(5, TimeUnit.SECONDS);
        assertThat(cdl.getCount(), is (0L));
        assertThat(resultHolder[0], is(dataToSend));

        MatcherAssert.assertThat(metrics.getMeter("websocket", "received", "echo").getCount(), is(1L));
    }

    @Configuration
    @Import({NovaProvidingConfiguration.class, WebSocketEnablingConfiguration.class})
    public static class MyConfig  {
        @Autowired
        Nova nova;
        @Autowired
        HttpServer httpServer;
        @Autowired
        AsyncHttpClient httpClient;

        @Bean
        public MyBean myBean() {
            return new MyBean();
        }

        @Bean
        public WebSocketAdapter<Integer> webSocketAdapter() {
            return WebSocketAdapter.builder(Integer.class)
                    .setHttpServer(httpServer)
                    .setHttpClient(httpClient)
                    .setMetrics(nova.metrics)
                    .build();
        }

    }

    public static class MyBean {
        @OnMessage("echo")
        public void websocketEchoInteger(Integer message, WebSocket<Integer> webSocket)  {
            webSocket.send(message);
        }
    }
}