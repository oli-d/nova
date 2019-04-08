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

import ch.squaredesk.nova.comm.http.HttpServerSettings;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class RestEnablingConfigurationTest {
    HttpServerSettings serverConfiguration;
    AnnotationConfigApplicationContext ctx;

    private ApplicationContext setupContext(Class configClass) throws Exception {
        ctx = new AnnotationConfigApplicationContext();
        ctx.register(configClass);
        ctx.refresh();
        serverConfiguration = ctx.getBean(HttpServerSettings.class);
        return ctx;
    }

    @BeforeAll
    static void setup() {
        System.setProperty("NOVA.REST.PACKAGES_TO_SCAN_FOR_HANDLERS", "ch.squaredesk.nova.comm.rest");
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty("NOVA.REST.PACKAGES_TO_SCAN_FOR_HANDLERS");
    }

    @BeforeEach
    void clearEnvironment() {
        System.clearProperty("NOVA.HTTP.REST.INTERFACE_NAME");
        System.clearProperty("NOVA.HTTP.REST.PORT");
        System.clearProperty("NOVA.REST.CAPTURE_METRICS");
        System.clearProperty("NOVA.HTTP.SERVER.AUTO_START");
    }

    @AfterEach
    void shutdown() throws Exception {
        ctx.getBean(HttpServer.class).shutdown().get();
    }

    @Test
    void ifNothingSpecifiedRestServerIsStartedWhenTheApplicationContextIsInitialized() throws Exception{
        ApplicationContext ctx = setupContext(MyConfig.class);
        assertThat(ctx.getBean("httpServer"), not(is(nullValue())));
        assertThat(ctx.getBean("httpServer", HttpServer.class).isStarted(), is(true));
    }

    @Test
    void autoRestServerStartingCanBeSwitchedOffWithEnvVariable() throws Exception{
        System.setProperty("NOVA.HTTP.SERVER.AUTO_START", "false");
        ApplicationContext ctx = setupContext(MyConfig.class);
        assertThat(ctx.getBean("httpServer"), not(is(nullValue())));
        assertThat(ctx.getBean("httpServer", HttpServer.class).isStarted(), is(false));
    }

    @Test
    void autoRestServerStartingCanBeSwitchedOffWithCustomBean() throws Exception{
        ApplicationContext ctx = setupContext(MyConfigWithNoAutostart.class);
        assertThat(ctx.getBean("httpServer"), not(is(nullValue())));
        assertThat(ctx.getBean("httpServer", HttpServer.class).isStarted(), is(false));
    }

    @Test
    void ifNothingSpecifiedMetricsAreCaptured() throws Exception{
        ApplicationContext ctx = setupContext(MyConfig.class);
        assertThat(ctx.getBean("captureRestMetrics"), is(true));
    }

    @Test
    void metricsCanBeSwitchedOffWithEnvVariable() throws Exception{
        System.setProperty("NOVA.REST.CAPTURE_METRICS", "false");
        ApplicationContext ctx = setupContext(MyConfig.class);
        assertThat(ctx.getBean("captureRestMetrics"), is(false));
    }

    @Test
    void ifNothingSpecifiedRestServerListensOnAllInterfaces() throws Exception{
        setupContext(MyConfig.class);
        assertThat(serverConfiguration.interfaceName, is("0.0.0.0"));
    }

    @Test
    void annotatedBeansAreAddedToResourceConfig() throws Exception{
        setupContext(MyConfigWithAnnotatedBean.class);
    }

    @Configuration
    @Import({RestTestConfig.class})
    public static class MyConfig {
    }

    @Configuration
    @Import({RestTestConfig.class})
    public static class MyConfigWithNoAutostart {
        @Bean("autoStartHttpServer")
        boolean myAutoStartBean() {
            return false;
        }
    }

    @Configuration
    @Import({RestTestConfig.class})
    public static class MyConfigWithAnnotatedBean {
        @Bean
        public MyDummyBeanToHaveAtLeastOneRestEndpoint dummyBeanToHaveAtLeastOneRestEndpoint() {
            return new MyDummyBeanToHaveAtLeastOneRestEndpoint();
        }
    }

    @Path("/")
    public static class MyDummyBeanToHaveAtLeastOneRestEndpoint {
        @Path("somePath")
        @GET
        public String foo() {
            return "foo";
        }

        @Path("someAdditionalPath")
        @GET
        public String bar() {
            return "bar";
        }
    }
}

