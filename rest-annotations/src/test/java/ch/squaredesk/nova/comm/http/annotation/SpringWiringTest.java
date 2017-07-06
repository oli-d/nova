/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http.annotation;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import ch.squaredesk.nova.comm.http.RpcServer;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import io.reactivex.BackpressureStrategy;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
        ctx.getBean(HttpServer.class).shutdown().get();
    }

    @Test
    public void restEndpointCanProperlyBeInvoked() throws Exception {
        setupContext(MyConfig.class);
        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/foo", null);
        assertThat(replyAsString, is("MyBean"));
    }

    @Test
    public void restAnnotationsCanBeMixedWithHttpRpcServer() throws Exception {
        setupContext(MyMixedConfig.class);
        ctx.getBean(HttpServer.class).start();
        RpcServer<String> rpcServer = ctx.getBean(RpcServer.class);
        rpcServer.requests("/bar", BackpressureStrategy.BUFFER).subscribe(
                rpcInvocation -> {
                    rpcInvocation.complete("bar");
                }
        );

        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/foo", null);
        assertThat(replyAsString, is("MyBean"));
        replyAsString = HttpHelper.getResponseBody(serverUrl + "/bar", null);
        assertThat(replyAsString, is("bar"));
    }

    @Configuration
    @Order
    @Import({RestEnablingConfiguration.class, NovaProvidingConfiguration.class, MyBeanConfig.class})
    public static class MyMixedConfig  {
        @Autowired
        ResourceConfig resourceConfig;

        @Autowired
        HttpServerConfiguration serverConfig;

        @Autowired
        public Nova nova;

        @Bean
        public HttpServer httpServer() {
            return RestServerFactory.serverFor(serverConfig, resourceConfig);
        }

        @Bean
        @Lazy
        public RpcServer<String> rpcServer() {
            return new RpcServer<>(httpServer(), s->s, s->s, nova.metrics );
        }
    }

    @Configuration
    @Import({RestServerProvidingConfiguration.class, MyBeanConfig.class})
    public static class MyConfig  {
    }

    @Configuration
    public static class MyBeanConfig  {
        @Bean
        public MyBean myBean() {
            return new MyBean();
        }
    }

    public static class MyBean {
        @OnRestRequest("/foo")
        public String restHandler()  {
            return "MyBean";
        }
    }
}