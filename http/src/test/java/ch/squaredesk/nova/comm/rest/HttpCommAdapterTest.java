/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpCommAdapterTest {
    private HttpCommAdapter<BigDecimal> sut;

    @BeforeEach
    void setup() {
        sut = HttpCommAdapter.<BigDecimal>builder()
                .setMessageMarshaller(BigDecimal::toString)
                .setMessageUnmarshaller(BigDecimal::new)
                .setErrorReplyFactory(t -> BigDecimal.ZERO)
                .setMetrics(new Metrics())
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
                .sendGetRequest("rest://cvf.bn.c")
                .test();
        observer.await(5, SECONDS);
        observer.assertError(Exception.class);
    }

    @Test
    void noReplyWithinTimeoutThrows() throws Exception {
        TestObserver<BigDecimal> observer = sut
                .sendGetRequest("rest://blick.ch",10L,MICROSECONDS)
                .test();
        observer.await(1, SECONDS);
        observer.assertError(TimeoutException.class);
    }

    @Test
    void postRequestCanBeSpecified() throws Exception {
        // we send a POST to httpbin/get and check that they return an error
        HttpCommAdapter<String> commAdapter = HttpCommAdapter.<String>builder()
                .setMessageMarshaller(outgoingMessage -> outgoingMessage)
                .setMessageUnmarshaller(incomingMessage -> incomingMessage)
                .setMetrics(new Metrics())
                .setErrorReplyFactory(t -> "Error: " + t.getMessage())
                .build();
        TestObserver<String> observer = commAdapter
                .sendPostRequest("rest://httpbin.org/get", "{ myTest: \"value\"}")
                .test();
        observer.await(5, SECONDS);
        observer.assertError(throwable -> throwable.getMessage().contains("METHOD NOT ALLOWED"));
    }

    @Test
    void getRequestCanBeSpecified() throws Exception {
        // we send a POST to httpbin/get and check that they return an error
        HttpCommAdapter<String> commAdapter = HttpCommAdapter.<String>builder()
                .setMessageMarshaller(outgoingMessage -> outgoingMessage)
                .setMessageUnmarshaller(incomingMessage -> incomingMessage)
                .setMetrics(new Metrics())
                .setErrorReplyFactory(t -> "Error: " + t.getMessage())
                .build();
        TestObserver<String> observer = commAdapter.sendGetRequest("rest://httpbin.org/post").test();
        observer.await(5, SECONDS);
        observer.assertError(throwable -> throwable.getMessage().contains("METHOD NOT ALLOWED"));
    }

    @Test
    void rpcWorksProperly() throws Exception {
        HttpCommAdapter<String> xxx = HttpCommAdapter.<String>builder()
                .setMessageMarshaller(outgoingMessage ->  outgoingMessage)
                .setMessageUnmarshaller(incomingMessage -> incomingMessage)
                .setMetrics(new Metrics())
                .setErrorReplyFactory(t -> "Error: " + t.getMessage())
                .build();
        TestObserver<String> observer = xxx
                .sendRequest("rest://httpbin.org/ip", "1", HttpRequestMethod.GET)
                .test();

        observer.await(5, SECONDS);
        observer.assertComplete();
        observer.assertValue(value -> value.contains("\"origin\":"));
    }

}