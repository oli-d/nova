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

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.tuples.Pair;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.reactivex.observers.TestObserver;
import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("medium")
class HttpAdapterTest {
    private HttpServerConfiguration rsc = HttpServerConfiguration.builder().interfaceName("localhost").port(10000).build();
    private HttpServer httpServer = HttpServerFactory.serverFor(rsc);
    private HttpAdapter sut;

    @BeforeEach
    void setup() {
        sut = HttpAdapter
                .builder()
                .setHttpServer(httpServer)
                .build();
    }

    @AfterEach
    void tearDown() {
        sut.shutdown();
    }

    @Test
    void sameAdapterCanBeUsedForDifferentMessageTypes() {
        String path = "/multiTypeTest";
        Pair<com.sun.net.httpserver.HttpServer, Integer> serverPortPair = httpServer(path, "1");
        String url = "http://localhost:" + serverPortPair._2 + path;

        TestObserver<RpcReply<String>> stringObserver = sut.sendGetRequest(url, String.class).test();
        TestObserver<RpcReply<Integer>> integerObserver = sut.sendGetRequest(url, Integer.class).test();
        TestObserver<RpcReply<Double>> doubleObserver = sut.sendGetRequest(url, Double.class).test();
        TestObserver<RpcReply<BigDecimal>> bigDecimalObserver = sut.sendGetRequest(url, BigDecimal.class).test();

        await().atMost(10, SECONDS).until(stringObserver::isTerminated, is(true));
        stringObserver.assertValue(reply -> "1".equals(reply.result));
        await().atMost(10, SECONDS).until(integerObserver::isTerminated, is(true));
        integerObserver.assertValue(reply -> 1 == reply.result);
        await().atMost(10, SECONDS).until(doubleObserver::isTerminated, is(true));
        doubleObserver.assertValue(reply -> 1.0 == reply.result);
        await().atMost(10, SECONDS).until(bigDecimalObserver::isTerminated, is(true));
        bigDecimalObserver.assertValue(reply -> BigDecimal.ONE.equals(reply.result));
    }

    @Test
    void oneOffTranscriberCanBeUsed() {
        String path = "/jacksonTest";
        Pair<com.sun.net.httpserver.HttpServer, Integer> serverPortPair = httpServer(path, "xxx");
        String url = "http://localhost:" + serverPortPair._2 + path;

        TestObserver<RpcReply<MyType1>> type1Observer = sut.sendGetRequest(url, MyType1::new).test();
        await().atMost(10, SECONDS).until(type1Observer::isTerminated, is(true));
        type1Observer.assertValue(reply -> new MyType1("xxx").equals(reply.result));

        assertThrows(
                IllegalArgumentException.class, () -> sut.sendGetRequest(url, MyType1.class));
    }

    @Test
    void parametersGetProperlyTransmitted() {
        Map<String, String> headers = new HashMap<>();
        headers.put("h1", "v1");
        headers.put("h2", "v2");
        Headers[] headersContainer = new Headers[1];
        String path = "/paramTest";
        Pair<com.sun.net.httpserver.HttpServer, Integer> serverPortPair = httpServer(
                path,
                "xxx",
                httpExchange -> {
                    headersContainer[0] = httpExchange.getRequestHeaders();
                });
        String url = "http://localhost:" + serverPortPair._2 + path;

        sut.sendGetRequest(url, headers, String.class);

        await().atMost(10, SECONDS).until(() -> headersContainer[0], not(nullValue()));
        MatcherAssert.assertThat(headersContainer[0].getFirst("h1"), is("v1"));
        MatcherAssert.assertThat(headersContainer[0].getFirst("h2"), is("v2"));
    }

    private Pair<com.sun.net.httpserver.HttpServer, Integer> httpServer(String path,
                                                                        String response) {
        return httpServer(path, response, exchange -> {
        });
    }

    private Pair<com.sun.net.httpserver.HttpServer, Integer> httpServer(String path,
                                                                        String response,
                                                                        Consumer<HttpExchange> requestHandler) {
        Pair<com.sun.net.httpserver.HttpServer, Integer>[] retVal = new Pair[1];

        PortFinder.withNextFreePort(port -> {
            com.sun.net.httpserver.HttpServer server = null;
            try {
                server = com.sun.net.httpserver.HttpServer
                        .create(new InetSocketAddress(port), 0);
                server.createContext(path, httpExchange -> {
                    requestHandler.accept(httpExchange);
                    httpExchange.sendResponseHeaders(200, response.length());
                    OutputStream os = httpExchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                });
                server.setExecutor(null); // creates a default executor
                server.start();
            } catch (IOException e) {
                fail(e);
            }
            retVal[0] = new Pair<>(server, port);
        });

        return retVal[0];
    }

    public static class MyType1 {
        public final String value;

        public MyType1(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MyType1 myType1 = (MyType1) o;
            return Objects.equals(value, myType1.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}