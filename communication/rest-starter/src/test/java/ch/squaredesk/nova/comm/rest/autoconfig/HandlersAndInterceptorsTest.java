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

package ch.squaredesk.nova.comm.rest.autoconfig;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.autoconfig.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.http.HttpRequestSender;
import ch.squaredesk.nova.comm.http.autoconfig.HttpAdapterAutoConfig;
import ch.squaredesk.nova.comm.http.autoconfig.HttpServerConfigurationProperties;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

class HandlersAndInterceptorsTest {
    private ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    NovaAutoConfiguration.class,
                    HttpAdapterAutoConfig.class,
                    RestAutoConfig.class))
            .withUserConfiguration(MyMixedConfig.class);


    @Test
    void restAnnotationsCanBeMixedWithHttpAdapterInServerMode() {
        applicationContextRunner
            .withPropertyValues("nova.http.server.port=" + PortFinder.findFreePort())
            .run(appContext -> {
                HttpServerConfigurationProperties serverSettings = appContext.getBean(HttpServerConfigurationProperties.class);
                int port = serverSettings.getPort();
                String serverUrl = "http://127.0.0.1:" + port;
                HandlersAndInterceptorsTest.MyRequestFilter myRequestFilter = appContext.getBean(HandlersAndInterceptorsTest.MyRequestFilter.class);
                HandlersAndInterceptorsTest.MyResponseFilter myResponseFilter = appContext.getBean(HandlersAndInterceptorsTest.MyResponseFilter.class);
                HandlersAndInterceptorsTest.MyWriterInterceptor myWriterInterceptor = appContext.getBean(HandlersAndInterceptorsTest.MyWriterInterceptor.class);

                MatcherAssert.assertThat(myRequestFilter.wasCalled, Matchers.is(false));
                MatcherAssert.assertThat(myResponseFilter.wasCalled, Matchers.is(false));
                MatcherAssert.assertThat(myWriterInterceptor.wasCalled, Matchers.is(false));

                String response = HttpRequestSender.sendPostRequest(serverUrl + "/foo", "some request")
                        .replyMessage;

                MatcherAssert.assertThat(response, Matchers.is("MyBean"));
                MatcherAssert.assertThat(myRequestFilter.wasCalled, Matchers.is(true));
                MatcherAssert.assertThat(myResponseFilter.wasCalled, Matchers.is(true));
                MatcherAssert.assertThat(myWriterInterceptor.wasCalled, Matchers.is(true));
            });
    }

    @Configuration
    public static class MyMixedConfig  {
        @Bean
        public MyBean myBean() {
            return new MyBean();
        }

        @Bean
        public MyRequestFilter myRequestFilter() {
            return new MyRequestFilter();
        }

        @Bean
        public MyResponseFilter myResponseFilter() {
            return new MyResponseFilter();
        }

        @Bean
        public MyWriterInterceptor myWriterInterceptor() {
            return new MyWriterInterceptor();
        }
    }

    @Path("/foo")
    public static class MyBean {
        @POST
        public String restHandler()  {
            return "MyBean";
        }
    }

    public static class MyRequestFilter implements ContainerRequestFilter {
        private boolean wasCalled = false;

        @Override
        public void filter(ContainerRequestContext requestContext) {
            wasCalled = true;
        }
    }

    public static class MyWriterInterceptor implements WriterInterceptor {
        private boolean wasCalled = false;

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            wasCalled = true;
            context.proceed();
        }
    }

    public static class MyResponseFilter implements ContainerResponseFilter {
        private boolean wasCalled = false;

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
            wasCalled = true;
        }
    }
}
