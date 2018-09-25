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

package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import ch.squaredesk.nova.comm.http.RpcServer;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

class SpringWiringTest {

    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    private String serverUrl;

    private void setupContext(Class configClass) throws Exception {
        ctx.register(configClass);
        ctx.refresh();
        HttpServerConfiguration cfg = ctx.getBean(HttpServerConfiguration.class);
        serverUrl = "http://" + cfg.interfaceName + ":" + cfg.port;
    }

    @AfterEach
    void tearDown() throws Exception {
        ctx.close();
    }

    @Test
    void restEndpointCanProperlyBeInvoked() throws Exception {
        setupContext(MyConfig.class);
        Metrics metrics = ctx.getBean(Nova.class).metrics;
        assertThat(metrics.getTimer("rest", "foo").getCount(), is(0L));

        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/foo", null);
        assertThat(replyAsString, is("MyBean"));
        assertThat(metrics.getTimer("rest", "foo").getCount(), is(1L));
    }

    @Test
    void restAnnotationsCanBeMixedWithHttpRpcServer() throws Exception {
        setupContext(MyMixedConfig.class);
        ctx.getBean(HttpServer.class).start();
        RpcServer<String> rpcServer = ctx.getBean(RpcServer.class);
        rpcServer.requests("/bar").subscribe(
                rpcInvocation -> {
                    rpcInvocation.complete("bar");
                }
        );

        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/bar", null);
        assertThat(replyAsString, is("bar"));
        replyAsString = HttpHelper.getResponseBody(serverUrl + "/foo", null);
        assertThat(replyAsString, is("MyBean"));
    }

    @Configuration
    @Order
    @Import({NovaProvidingConfiguration.class, RestEnablingConfiguration.class})
    public static class MyMixedConfig  {
        @Autowired
        ApplicationContext applicationContext;

        @Autowired
        public Nova nova;

        @Bean
        public MyBean myBean() {
            return new MyBean();
        }

        @Bean
        @Lazy
        public RpcServer<String> rpcServer() {
            HttpServer httpServer = applicationContext.getBean(HttpServer.class);
            return new RpcServer<>("SWT", httpServer, s->s, s->s, nova.metrics );
        }
    }

    @Configuration
    @Import({NovaProvidingConfiguration.class, RestEnablingConfiguration.class})
    public static class MyConfig  {
        @Bean
        public MyBean myBean() {
            return new MyBean();
        }

    }

    @Path("/foo")
    public static class MyBean {
        @GET
        public String restHandler()  {
            return "MyBean";
        }
    }
}