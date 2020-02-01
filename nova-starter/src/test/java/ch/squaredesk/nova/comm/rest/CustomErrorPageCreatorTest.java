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
import ch.squaredesk.nova.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.http.HttpAdapterAutoConfig;
import ch.squaredesk.nova.comm.http.HttpRequestSender;
import ch.squaredesk.nova.comm.http.HttpServerConfigurationProperties;
import ch.squaredesk.nova.comm.http.HttpServerSettings;
import ch.squaredesk.nova.metrics.Metrics;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.Request;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class CustomErrorPageCreatorTest {
    private ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    NovaAutoConfiguration.class,
                    HttpAdapterAutoConfig.class,
                    RestAutoConfig.class))
            .withUserConfiguration(CustomErrorPageCreatorTest.MyConfig.class)
            .withPropertyValues("nova.http.server.port=" + PortFinder.findFreePort());

    @Test
    void uncaughtErrorInRestEndpointInvokesErrorPageGenerator() throws Exception {
        applicationContextRunner
                .run(appContext -> {
                    HttpServerConfigurationProperties httpServerSettings = appContext.getBean(HttpServerConfigurationProperties.class);
                    MyErrorPageGenerator myErrorPageGenerator = appContext.getBean(MyErrorPageGenerator.class);;

                    String serverUrl = "http://127.0.0.1:" + httpServerSettings.getPort();

                    HttpRequestSender.HttpResponse reply = HttpRequestSender.sendGetRequest(serverUrl + "/foo?query=x'WHERE%20full_name%20like'%BOB%");
                    assertThat(reply.returnCode, is(500));
                    assertThat(reply.replyMessage, is("Error"));
                    assertThat(myErrorPageGenerator.wasCalled, is(true));
                });
    }

    @Configuration
    public static class MyConfig  {
        @Bean
        public MyBean myBean() {
            return new MyBean();
        }

        @Bean
        public MyErrorPageGenerator myErrorPageGenerator() {
            return new MyErrorPageGenerator();
        }

    }

    @Path("/foo")
    public static class MyBean {
        @GET
        public String restHandler(@QueryParam("query") String query)  {
            return "Hello, I should never be called in this test";
        }
    }

    public static class MyErrorPageGenerator implements ErrorPageGenerator {
        private boolean wasCalled = false;
        @Override
        public String generate(Request request, int status, String reasonPhrase, String description, Throwable exception) {
            wasCalled = true;
            return "Error";
        }
    }
}