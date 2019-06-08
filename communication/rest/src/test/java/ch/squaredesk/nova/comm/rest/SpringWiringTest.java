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
import ch.squaredesk.nova.comm.http.HttpServerSettings;
import ch.squaredesk.nova.comm.http.RpcServer;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SpringWiringTest.MyConfig.class})
class SpringWiringTest {
    @Autowired
    HttpServerSettings httpServerSettings;
    @Autowired
    Nova nova;

    @Test
    void restEndpointCanProperlyBeInvoked() throws Exception {
        String serverUrl = "http://127.0.0.1:" + httpServerSettings.port;
        Metrics metrics = nova.metrics;

        assertThat(metrics.getTimer("rest", "foo").getCount(), is(0L));

        String replyAsString = HttpHelper.getResponseBody(serverUrl + "/foo", null);
        assertThat(replyAsString, is("MyBean"));
        assertThat(metrics.getTimer("rest", "foo").getCount(), is(1L));
    }

    @Configuration
    @Import({RestTestConfig.class})
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