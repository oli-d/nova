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

import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class RestServerProvidingConfigurationTest {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    private void setupContext() throws Exception {
        ctx.register(MyConfig.class);
        ctx.refresh();
    }

    @AfterEach
    void tearDown() throws Exception {
        ctx.getBean(HttpServer.class).shutdown();
    }

    @Test
    void serverIsAutomaticallyStartedAndEndpointsAreRegistered() throws Exception {
        setupContext();
        MatcherAssert.assertThat(HttpHelper.getResponseBody("http://localhost:8080/foo",null), Matchers.is("foo"));
    }

    @Configuration
    @Import(RestServerProvidingConfiguration.class)
    public static class MyConfig {
        @Bean
        public MyDummyBeanToHaveAtLeastOneRestEndpoint dummyBeanToHaveAtLeastOneRestEndpoint() {
            return new MyDummyBeanToHaveAtLeastOneRestEndpoint();
        }
    }

    public static class MyDummyBeanToHaveAtLeastOneRestEndpoint {
        @OnRestRequest("/foo")
        public String foo() {
            return "foo";
        }
    }
}

