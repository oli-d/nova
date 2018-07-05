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

import ch.squaredesk.nova.comm.http.HttpServerConfiguration;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpEnablingConfigurationTest {
    HttpServerConfiguration serverConfiguration;
    ResourceConfig resourceConfig;
    AnnotationConfigApplicationContext ctx;

    private ApplicationContext setupContext(Class configClass) throws Exception {
        ctx = new AnnotationConfigApplicationContext();
        ctx.register(configClass);
        ctx.refresh();
        serverConfiguration = ctx.getBean(HttpServerConfiguration.class);
        resourceConfig = ctx.getBean(RestBeanPostprocessor.class).resourceConfig;
        return ctx;
    }

    @BeforeEach
    void clearEnvironment() {
        System.clearProperty("NOVA.HTTP.REST.INTERFACE_NAME");
        System.clearProperty("NOVA.HTTP.REST.PORT");
        System.clearProperty("NOVA.HTTP.REST.CAPTURE_METRICS");
    }

    @AfterEach
    void shutdown() throws Exception {
        ctx.getBean(HttpServer.class).shutdown().get();
    }

    @Test
    void ifNothingSpecifiedMetricsAreCaptured() throws Exception{
        ApplicationContext ctx = setupContext(MyConfig.class);
        assertThat(ctx.getBean("captureRestMetrics"), is(true));
    }

    @Test
    void metricsCanBeSwitchedOffWithEnvVariable() throws Exception{
        System.setProperty("NOVA.HTTP.REST.CAPTURE_METRICS", "false");
        ApplicationContext ctx = setupContext(MyConfig.class);
        assertThat(ctx.getBean("captureRestMetrics"), is(false));
    }

    @Test
    void ifNothingSpecifiedRestServerListensOn10000OnAllInterfaces() throws Exception{
        setupContext(MyConfig.class);
        assertThat(serverConfiguration.port, is(10000));
        assertThat(serverConfiguration.interfaceName, is("0.0.0.0"));
        assertTrue(resourceConfig.getResources().isEmpty());
    }

    @Test
    void annotatedBeansAreAddedToResourceConfig() throws Exception{
        setupContext(MyConfigWithAnnotatedBean.class);
        assertThat(resourceConfig.getResources().size(), is(2));
    }

    @Configuration
    @Import({RestEnablingConfiguration.class, NovaProvidingConfiguration.class })
    public static class MyConfig {
    }

    @Configuration
    @Import({RestEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class MyConfigWithAnnotatedBean {
        @Bean
        public MyDummyBeanToHaveAtLeastOneRestEndpoint dummyBeanToHaveAtLeastOneRestEndpoint() {
            return new MyDummyBeanToHaveAtLeastOneRestEndpoint();
        }
    }

    public static class MyDummyBeanToHaveAtLeastOneRestEndpoint {
        @OnRestRequest("somePath")
        public void foo() {
        }

        @OnRestRequest("someAdditionalPath")
        public void bar() {
        }
    }
}

