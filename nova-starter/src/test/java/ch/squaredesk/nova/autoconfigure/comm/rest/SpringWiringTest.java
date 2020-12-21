/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.rest;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.autoconfigure.comm.http.HttpAdapterAutoConfiguration;
import ch.squaredesk.nova.autoconfigure.comm.http.HttpClientAutoConfiguration;
import ch.squaredesk.nova.autoconfigure.comm.http.HttpServerAutoConfiguration;
import ch.squaredesk.nova.autoconfigure.core.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.http.HttpRequestSender;
import ch.squaredesk.nova.autoconfigure.comm.http.HttpServerConfigurationProperties;
import ch.squaredesk.nova.metrics.Metrics;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

class SpringWiringTest {
    private ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestAutoConfiguration.class, HttpAdapterAutoConfiguration.class, HttpServerAutoConfiguration.class, HttpClientAutoConfiguration.class, NovaAutoConfiguration.class))
            .withUserConfiguration(MyConfig.class)
            .withPropertyValues("nova.http.server.port=" + PortFinder.findFreePort());

    @Test
    void restEndpointCanProperlyBeInvoked() {
        applicationContextRunner
            .run(appContext -> {
                Nova nova = appContext.getBean(Nova.class);
                Metrics metrics = nova.metrics;
                HttpServerConfigurationProperties serverSettings = appContext.getBean(HttpServerConfigurationProperties.class);
                int port = serverSettings.getPort();
                String serverUrl = "http://127.0.0.1:" + port;

                MatcherAssert.assertThat(metrics.getTimer("rest", "foo").getCount(), Matchers.is(0L));

                String replyAsString = HttpRequestSender.sendGetRequest(serverUrl + "/foo").replyMessage;
                MatcherAssert.assertThat(replyAsString, Matchers.is("MyBean"));
                MatcherAssert.assertThat(metrics.getTimer("rest", "foo").getCount(), Matchers.is(1L));
            });
    }

    @Configuration
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