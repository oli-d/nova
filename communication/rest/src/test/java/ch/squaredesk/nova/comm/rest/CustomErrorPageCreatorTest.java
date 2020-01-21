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

import ch.squaredesk.nova.comm.http.HttpRequestSender;
import ch.squaredesk.nova.comm.http.HttpServerSettings;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CustomErrorPageCreatorTest.MyConfig.class})
class CustomErrorPageCreatorTest {
    @Autowired
    HttpServerSettings httpServerSettings;
    @Autowired
    MyErrorPageGenerator myErrorPageGenerator;

    @Test
    void uncaughtErrorInRestEndpointInvokesErrorPageGenerator() throws Exception {
        String serverUrl = "http://127.0.0.1:" + httpServerSettings.port;

        HttpRequestSender.HttpResponse reply = HttpRequestSender.sendGetRequest(serverUrl + "/foo?query=x'WHERE%20full_name%20like'%BOB%");
        assertThat(reply.returnCode, is(500));
        assertThat(reply.replyMessage, is("Error"));
        assertThat(myErrorPageGenerator.wasCalled, is(true));
    }

    @Configuration
    @Import({RestTestConfig.class})
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