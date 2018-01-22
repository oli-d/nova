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

package ch.squaredesk.nova.comm.http;

import io.reactivex.observers.TestObserver;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpAdapterTest {
    private HttpServerConfiguration rsc = HttpServerConfiguration.builder().interfaceName("localhost").port(10000).build();
    private HttpServer httpServer = HttpServerFactory.serverFor(rsc);
    private HttpAdapter<BigDecimal> sut;

    @BeforeEach
    void setup() {
        sut = HttpAdapter.builder(BigDecimal.class)
                .setHttpServer(httpServer)
                .build();

    }

    @Test
    void nullDestinationThrows() {
        Throwable t = assertThrows(IllegalArgumentException.class,
                () -> sut.sendRequest(null,new BigDecimal("1.0")));
        assertThat(t.getMessage(), containsString("Invalid URL format"));
    }

    @Test
    void invalidDestinationFormatThrows() {
        Throwable t = assertThrows(IllegalArgumentException.class,
                () -> sut.sendRequest("\\ßö ", new BigDecimal("1.0")));
        assertThat(t.getMessage(), containsString("Invalid URL format"));
    }

    @Test
    void notExistingDestinationThrows() throws Exception {
        TestObserver<BigDecimal> observer = sut
                .sendGetRequest("http://cvf.bn.c")
                .test();
        observer.await(5, SECONDS);
        observer.assertError(Exception.class);
    }

    @Test
    void noReplyWithinTimeoutThrows() throws Exception {
        TestObserver<BigDecimal> observer = sut
                .sendGetRequest("https://www.nytimes.com",10L,MICROSECONDS)
                .test();
        observer.await(1, SECONDS);
        observer.assertError(ExecutionException.class);
        Assertions.assertTrue(observer.errors().get(0).getCause() instanceof TimeoutException);
    }

    @Test
    void postRequestCanBeSpecified() throws Exception {
        // we send a POST to httpbin/get and check that they return an error
        HttpAdapter<String> commAdapter = HttpAdapter.builder(String.class)
                .setHttpServer(httpServer)
                .build();
        TestObserver<String> observer = commAdapter
                .sendPostRequest("http://httpbin.org/get", "{ myTest: \"value\"}")
                .test();
        observer.await(40, SECONDS);
        observer.assertError(throwable -> throwable.getMessage().contains("METHOD NOT ALLOWED"));
    }

    @Test
    void getRequestCanBeSpecified() throws Exception {
        // we send a POST to httpbin/get and check that they return an error
        HttpAdapter<String> commAdapter = HttpAdapter.builder(String.class)
                .setHttpServer(httpServer)
                .build();
        TestObserver<String> observer = commAdapter.sendGetRequest("http://httpbin.org/post").test();
        observer.await(40, SECONDS);
        observer.assertError(throwable -> throwable.getMessage().contains("METHOD NOT ALLOWED"));
    }

    @Test
    void rpcWorksProperly() throws Exception {
        HttpAdapter<String> xxx = HttpAdapter.builder(String.class)
                .setHttpServer(httpServer)
                .build();
        TestObserver<String> observer = xxx
                .sendRequest("http://httpbin.org/ip", "1", HttpRequestMethod.GET)
                .test();

        observer.await(40, SECONDS);
        observer.assertComplete();
        observer.assertValue(value -> value.contains("\"origin\":"));
    }

}