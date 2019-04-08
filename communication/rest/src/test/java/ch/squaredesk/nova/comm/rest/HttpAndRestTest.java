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

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpServerSettings;
import ch.squaredesk.nova.comm.http.RpcServer;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {HttpAndRestTest.MyMixedConfig.class})
class HttpAndRestTest {
    @Autowired
    HttpServerSettings httpServerSettings;
    @Autowired
    RpcServer rpcServer;


    @Test
    void restAnnotationsCanBeMixedWithHttpRpcServer() throws Exception {
        String serverUrl = "http://127.0.0.1:" + httpServerSettings.port;
        rpcServer.requests("/bar", String.class).subscribe(
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
    @Import({RestTestConfig.class})
    public static class MyMixedConfig  {
        @Bean
        public MyBean myBean() {
            return new MyBean();
        }

        @Bean
        public RpcServer rpcServer(HttpServer httpServer, Nova nova) {
            return new RpcServer("SWT", httpServer, nova.metrics );
        }
    }

    @Path("/foo2")
    public static class MyBean {
        @GET
        public String restHandler()  {
            return "MyBean";
        }
    }
}