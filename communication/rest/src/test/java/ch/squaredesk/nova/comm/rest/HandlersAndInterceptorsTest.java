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

import ch.squaredesk.nova.comm.http.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.*;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HandlersAndInterceptorsTest.MyMixedConfig.class })
class HandlersAndInterceptorsTest {
    @Autowired
    HttpServerSettings httpServerSettings;
    @Autowired
    HandlersAndInterceptorsTest.MyRequestFilter myRequestFilter;
    @Autowired
    HandlersAndInterceptorsTest.MyResponseFilter myResponseFilter;
    @Autowired
    HandlersAndInterceptorsTest.MyWriterInterceptor myWriterInterceptor;


    @Test
    void restAnnotationsCanBeMixedWithHttpAdapterInServerMode() throws Exception {
        String serverUrl = "http://127.0.0.1:" + httpServerSettings.port;
        assertThat(myRequestFilter.wasCalled, is(false));
        assertThat(myResponseFilter.wasCalled, is(false));
        assertThat(myWriterInterceptor.wasCalled, is(false));

        String response = HttpRequestSender.sendPostRequest(serverUrl + "/foo", "some request")
                .replyMessage;

        assertThat(response, is("MyBean"));
        assertThat(myRequestFilter.wasCalled, is(true));
        assertThat(myResponseFilter.wasCalled, is(true));
        assertThat(myWriterInterceptor.wasCalled, is(true));
    }

    @Configuration
    @Import({RestTestConfig.class})
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
