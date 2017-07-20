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
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
        Metrics metrics = ctx.getBean(Nova.class).metrics;
        assertThat(metrics.getTimer("rest","foo").getCount(), is(0L));

        assertThat(HttpHelper.getResponseBody("http://localhost:10000/foo",null), is("foo"));
        assertThat(metrics.getTimer("rest","foo").getCount(), is(1L));
    }

    @Configuration
    @Import({RestServerProvidingConfiguration.class, NovaProvidingConfiguration.class})
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

