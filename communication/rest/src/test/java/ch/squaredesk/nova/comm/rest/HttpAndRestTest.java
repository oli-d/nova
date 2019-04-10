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

import ch.squaredesk.nova.comm.http.HttpAdapter;
import ch.squaredesk.nova.comm.http.HttpServerSettings;
import ch.squaredesk.nova.comm.http.RpcReply;
import io.reactivex.observers.TestObserver;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {HttpAndRestTest.MyMixedConfig.class})
class HttpAndRestTest {
    @Autowired
    HttpServerSettings httpServerSettings;
    @Autowired
    HttpAdapter httpAdapter;


    @Test
    void restAnnotationsCanBeMixedWithHttpAdapterInClientMode() throws Exception {
        String serverUrl = "http://127.0.0.1:" + httpServerSettings.port;
        TestObserver<RpcReply<String>> test = httpAdapter.sendGetRequest(serverUrl + "/foo2", String.class).test();

        assertThat(test.valueCount(), is(1));
        assertThat(test.values().get(0).result, is("MyBean"));
    }

    @Configuration
    @Import({RestTestConfig.class})
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