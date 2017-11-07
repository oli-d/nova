/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import ch.squaredesk.nova.comm.http.RpcServer;
import ch.squaredesk.nova.comm.websockets.annotation.OnMessage;
import ch.squaredesk.nova.comm.websockets.annotation.WebSocket;
import ch.squaredesk.nova.comm.websockets.annotation.WebSocketEnablingConfiguration;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import io.reactivex.BackpressureStrategy;
import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class SpringWiringTest {

    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    private String serverUrl;

    private void setupContext(Class configClass) throws Exception {
        ctx.register(configClass);
        ctx.refresh();
        HttpServerConfiguration cfg = ctx.getBean(HttpServerConfiguration.class);
        serverUrl = "http://" + cfg.interfaceName + ":" + cfg.port;
    }

    @AfterEach
    public void tearDown() throws Exception {
        ctx.close();
    }

    @Test
    public void webSocketEndpointCanProperlyBeInvoked() throws Exception {
        setupContext(MyConfig.class);
        Metrics metrics = ctx.getBean(Nova.class).metrics;
        MatcherAssert.assertThat(metrics.getTimer("rest", "foo").getCount(), is(0L));

//        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/foo", null);
//        assertThat(replyAsString, is("MyBean"));
        MatcherAssert.assertThat(metrics.getTimer("rest", "foo").getCount(), is(1L));
    }

    @Configuration
    @Import({NovaProvidingConfiguration.class, WebSocketEnablingConfiguration.class})
    public static class MyConfig  {
        @Bean
        public MyBean myBean() {
            return new MyBean();
        }

    }

    public static class MyBean {
        @OnMessage("/foo")
        public void websocketHello(String message, WebSocket<String> webSocket)  {
            webSocket.send("Hello: " + message);
        }
    }
}