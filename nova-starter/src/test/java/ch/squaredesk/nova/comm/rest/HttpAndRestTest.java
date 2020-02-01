/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
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
import ch.squaredesk.nova.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.http.*;
import io.reactivex.observers.TestObserver;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static org.hamcrest.Matchers.is;

class HttpAndRestTest {
    private ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    NovaAutoConfiguration.class,
                    HttpAdapterAutoConfig.class,
                    RestAutoConfig.class));


    @Test
    void restAnnotationsCanBeMixedWithHttpAdapterInClientMode() {
        applicationContextRunner
                .withUserConfiguration(MyMixedConfig.class)
                .withPropertyValues("nova.http.server.port=" + PortFinder.findFreePort())
                .run(appContext -> {
                    HttpServerConfigurationProperties serverSettings = appContext.getBean(HttpServerConfigurationProperties.class);
                    int port = serverSettings.getPort();
                    HttpAdapter httpAdapter = appContext.getBean(HttpAdapter.class);

                    String serverUrl = "http://127.0.0.1:" + port;
                    TestObserver<RpcReply<String>> test = httpAdapter.sendGetRequest(serverUrl + "/foo2", String.class).test();

                    Awaitility.await().atMost(Duration.FIVE_SECONDS).until(test::valueCount, is(1));
                    MatcherAssert.assertThat(test.valueCount(), Matchers.is(1));
                    MatcherAssert.assertThat(test.values().get(0).result, Matchers.is("MyBean"));
                });
    }

    @Test
    void restAnnotationsCanBeMixedWithHttpAdapterInServerMode() {
        applicationContextRunner
                .withUserConfiguration(MyMixedConfig.class)
                .run(appContext -> {
                    HttpServerConfigurationProperties serverSettings = appContext.getBean(HttpServerConfigurationProperties.class);
                    int port = serverSettings.getPort();
                    HttpAdapter httpAdapter = appContext.getBean(HttpAdapter.class);
                    Awaitility.await().atMost(Duration.FIVE_SECONDS).until(httpAdapter::isServerStarted);

                    String serverUrl = "http://127.0.0.1:" + port;
                    httpAdapter.requests("/foo1", String.class).subscribe(
                            invocation -> invocation.complete("YourBean", 200)
                    );

                    String response1 = HttpRequestSender.sendGetRequest(serverUrl + "/foo1").replyMessage;
                    String response2 = HttpRequestSender.sendGetRequest(serverUrl + "/foo2").replyMessage;

                    MatcherAssert.assertThat(response1, Matchers.is("YourBean"));
                    MatcherAssert.assertThat(response2, Matchers.is("MyBean"));
                });
    }

    @Configuration
    public static class MyMixedConfig  {
        @Bean
        public MyBean myBean() {
            return new MyBean();
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