/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.rest;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.autoconfigure.comm.http.HttpAdapterAutoConfiguration;
import ch.squaredesk.nova.autoconfigure.comm.http.HttpClientAutoConfiguration;
import ch.squaredesk.nova.autoconfigure.comm.http.HttpServerAutoConfiguration;
import ch.squaredesk.nova.autoconfigure.comm.http.HttpServerConfigurationProperties;
import ch.squaredesk.nova.autoconfigure.core.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.http.HttpRequestSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class RestInvocationLoggerTest {
    private ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestAutoConfiguration.class, HttpAdapterAutoConfiguration.class, HttpServerAutoConfiguration.class, HttpClientAutoConfiguration.class, NovaAutoConfiguration.class))
            .withUserConfiguration(RestInvocationLoggerTest.MyConfig.class)
            .withPropertyValues("nova.http.server.port=" + PortFinder.findFreePort());

    @AfterEach
    void tearDown() {
        System.setOut(null);
    }


    @Test
    void loggingCanBeDisabledWithAnnotation() {
        applicationContextRunner
                .run(appContext -> {
                    HttpServerConfigurationProperties serverSettings = appContext.getBean(HttpServerConfigurationProperties.class);
                    int port = serverSettings.getPort();
                    String serverUrl = "http://127.0.0.1:" + port;
                    ByteArrayOutputStream outputBuffer = redirectSysoutToStream();

                    String replyAsString = HttpRequestSender.sendPostRequest(serverUrl + "/noLog", "request").replyMessage;
                    String logOutput = outputBuffer.toString("UTF-8");

                    assertThat(replyAsString, is("MyBeanWithoutLogging"));
                    assertThat(logOutput.contains("handler for path"), is(false));
                    assertThat(logOutput.contains("Request headers:"), is(false));
                    assertThat(logOutput.contains("Request body:"), is(false));
                });
    }

    @Test
    void noAnnotationUsesDefaultSettings() {
        applicationContextRunner
                .run(appContext -> {
                    HttpServerConfigurationProperties serverSettings = appContext.getBean(HttpServerConfigurationProperties.class);
                    int port = serverSettings.getPort();
                    String serverUrl = "http://127.0.0.1:" + port;
                    ByteArrayOutputStream outputBuffer = redirectSysoutToStream();

                    String replyAsString = HttpRequestSender.sendPostRequest(serverUrl + "/noDef", "request").replyMessage;
                    String logOutput = outputBuffer.toString("UTF-8");

                    assertThat(replyAsString, is("MyBeanWithoutAnnotation"));
                    assertThat(logOutput.contains("DEBUG ch.squaredesk.nova.autoconfigure.comm.rest.RestInvocationLogger - Invoking POST handler for path"), is(true));
                    assertThat(logOutput.contains("/noDef"), is(true));
                    assertThat(logOutput.contains("Request headers:"), is(true));
                    assertThat(logOutput.contains("Request body:"), is(false));
                });
    }

    @Test
    void headerLoggingCanBeDisabled() {
        applicationContextRunner
                .run(appContext -> {
                    HttpServerConfigurationProperties serverSettings = appContext.getBean(HttpServerConfigurationProperties.class);
                    int port = serverSettings.getPort();
                    String serverUrl = "http://127.0.0.1:" + port;
                    ByteArrayOutputStream outputBuffer = redirectSysoutToStream();

                    String replyAsString = HttpRequestSender.sendPostRequest(serverUrl + "/noHeaderLog", "request").replyMessage;
                    String logOutput = outputBuffer.toString("UTF-8");

                    assertThat(replyAsString, is("MyBeanWithoutHeaderLogging"));
                    assertThat(logOutput.contains("DEBUG ch.squaredesk.nova.autoconfigure.comm.rest.RestInvocationLogger - Invoking POST handler for path"), is(true));
                    assertThat(logOutput.contains("/noHeaderLog"), is(true));
                    assertThat(logOutput.contains("Request headers:"), is(false));
                    assertThat(logOutput.contains("Request body:"), is(false));
                });
    }

    @Test
    void logLevelCanBeDefined() {
        applicationContextRunner
                .run(appContext -> {
                    HttpServerConfigurationProperties serverSettings = appContext.getBean(HttpServerConfigurationProperties.class);
                    int port = serverSettings.getPort();
                    String serverUrl = "http://127.0.0.1:" + port;
                    ByteArrayOutputStream outputBuffer = redirectSysoutToStream();

                    String replyAsString = HttpRequestSender.sendGetRequest(serverUrl + "/errorLog?query=oli&query2=somethingElse").replyMessage;
                    String logOutput = outputBuffer.toString("UTF-8");

                    assertThat(replyAsString, is("MyBeanWithErrorLogging"));
                    assertThat(logOutput.contains("ERROR ch.squaredesk.nova.autoconfigure.comm.rest.RestInvocationLogger - Invoking GET handler for path"), is(true));
                    assertThat(logOutput.contains("/errorLog?query=oli&query2=somethingElse"), is(true));
                    assertThat(logOutput.contains("Request headers:"), is(true));
                    assertThat(logOutput.contains("Request body:"), is(false));
                });
    }

    @Test
    void invalidLogLevelFallsBackToDebug() {
        applicationContextRunner
                .run(appContext -> {
                    HttpServerConfigurationProperties serverSettings = appContext.getBean(HttpServerConfigurationProperties.class);
                    int port = serverSettings.getPort();
                    String serverUrl = "http://127.0.0.1:" + port;
                    ByteArrayOutputStream outputBuffer = redirectSysoutToStream();

                    String replyAsString = HttpRequestSender.sendPostRequest(serverUrl + "/invalidLevel", "request").replyMessage;
                    String logOutput = outputBuffer.toString("UTF-8");

                    assertThat(replyAsString, is("MyBeanWithInvalidLevel"));
                    assertThat(logOutput.contains("DEBUG ch.squaredesk.nova.autoconfigure.comm.rest.RestInvocationLogger - Invoking POST handler for path"), is(true));
                    assertThat(logOutput.contains("/invalidLevel"), is(true));
                    assertThat(logOutput.contains("Request headers:"), is(true));
                    assertThat(logOutput.contains("Request body:"), is(false));
                });
    }

    private ByteArrayOutputStream redirectSysoutToStream() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        return buffer;
    }

    @Configuration
    public static class MyConfig {
        @Bean
        public MyBeanWithoutLogging myBeanWithoutLogging() {
            return new MyBeanWithoutLogging();
        }

        @Bean
        public MyBeanWithErrorLogging myBeanWithErrorLogging() {
            return new MyBeanWithErrorLogging();
        }

        @Bean
        public MyBeanWithoutAnnotation myBeanWithoutAnnotation() {
            return new MyBeanWithoutAnnotation();
        }

        @Bean
        public MyBeanWithoutHeaderLogging myBeanWithoutHeaderLogging() {
            return new MyBeanWithoutHeaderLogging();
        }

        @Bean
        public MyBeanWithInvalidLevel myBeanWithInvalidLevel() {
            return new MyBeanWithInvalidLevel();
        }

    }

    @Path("/noDef")
    public static class MyBeanWithoutAnnotation {
        @POST
        public String restHandler(String body) {
            return "MyBeanWithoutAnnotation";
        }
    }

    @Path("/noLog")
    public static class MyBeanWithoutLogging {
        @POST
        @InvocationLog(enabled = false )
        public String restHandler(String body) {
            return "MyBeanWithoutLogging";
        }
    }

    @Path("/errorLog")
    public static class MyBeanWithErrorLogging {
        @GET
        @InvocationLog(logLevel = "ERROR" )
        public String restHandler() {
            return "MyBeanWithErrorLogging";
        }
    }

    @Path("/noHeaderLog")
    public static class MyBeanWithoutHeaderLogging {
        @POST
        @InvocationLog(logHeaders = false)
        public String restHandler(String body) {
            return "MyBeanWithoutHeaderLogging";
        }
    }

    @Path("/invalidLevel")
    public static class MyBeanWithInvalidLevel {
        @POST
        @InvocationLog(logLevel = "OLI")
        public String restHandler(String body) {
            return "MyBeanWithInvalidLevel";
        }
    }

}