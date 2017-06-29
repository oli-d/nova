/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest.annotation;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.rest.HttpHelper;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

class RestEnablingConfigurationTest {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    private void fireUp() throws Exception {
        ctx.register(MyConfig.class);
        ctx.refresh();
        ctx.getBean(HttpServer.class).start();
    }

    @BeforeEach
    void clearEnvironment() {
        System.clearProperty("NOVA.HTTP.REST.INTERFACE_NAME");
        System.clearProperty("NOVA.HTTP.REST.PORT");
    }

    @AfterEach
    void tearDown() throws Exception {
        ctx.getBean(HttpServer.class).shutdown();
    }

    @Test
    void instanceIsCreatedWithDefaultValuesWhenJustImportingConfig() throws Exception {
        fireUp();
        HttpHelper.waitUntilSomebodyListensOnPort(8888, 2, TimeUnit.SECONDS);
    }

    @Test
    void portCanBeOverridenWithEnvironmentVariable() throws Exception{
        System.setProperty("NOVA.HTTP.REST.PORT", "9999");
        fireUp();
        HttpHelper.waitUntilSomebodyListensOnPort(9999, 2, TimeUnit.SECONDS);
    }

    @Configuration
    @Import(RestEnablingConfiguration.class)
    public static class MyConfig {
        @Bean
        public Nova nova () {
            return Nova.builder().build();
        }

        @Bean
        public MyDummyBeanToHaveAtLeastOneRestEndpoint dummyBeanToHaveAtLeastOneRestEndpoint() {
            return new MyDummyBeanToHaveAtLeastOneRestEndpoint();
        }
    }

    public static class MyDummyBeanToHaveAtLeastOneRestEndpoint {
        @OnRestRequest("somePath")
        public void foo() {
        }
    }
}

