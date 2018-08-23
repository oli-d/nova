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
import com.sun.net.httpserver.HttpExchange;
import io.reactivex.observers.TestObserver;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@Tag("medium")
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

    @AfterEach
    void tearDown() {
        sut.shutdown();
    }

    @Test
    void nullDestinationThrows() {
        Throwable t = assertThrows(IllegalArgumentException.class,
                () -> sut.sendRequest(null, new BigDecimal("1.0")));
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
        TestObserver<RpcReply<BigDecimal>> observer = sut
                .sendGetRequest("http://cvf.bn.c")
                .test();
        observer.await(5, SECONDS);
        observer.assertError(Exception.class);
    }

    @Test
    void noReplyWithinTimeoutThrows() throws Exception {
        HttpAdapter<String> commAdapter = HttpAdapter.builder(String.class).build();
        Pair<com.sun.net.httpserver.HttpServer, Integer> serverPortPair =
                httpServer("/timeoutTest", "noResponse", httpExchange -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

        try {
            TestObserver<RpcReply<String>> observer = commAdapter
                    .sendGetRequest("http://localhost:" + serverPortPair._2 + "/timeoutTest", 10l, MILLISECONDS)
                    .test();
            observer.await(1, SECONDS);
            observer.assertError(TimeoutException.class);
        } finally {
            serverPortPair._1.stop(0);
            commAdapter.shutdown();
        }
    }

    @Test
    void postRequestCanBeSpecified() throws Exception {
        HttpAdapter<String> commAdapter = HttpAdapter.builder(String.class).build();
        String[] requestMethodHolder = new String[1];
        Pair<com.sun.net.httpserver.HttpServer, Integer> serverPortPair =
                httpServer("/postTest", "myPostResponse", httpExchange -> requestMethodHolder[0] = httpExchange.getRequestMethod());

        try {
            TestObserver<RpcReply<String>> observer = commAdapter
                    .sendPostRequest("http://localhost:"+ serverPortPair._2 + "/postTest", "{ myTest: \"value\"}")
                    .test();
            await().atMost(40, SECONDS).until(observer::valueCount, is(1));
            RpcReply<String> reply = observer.values().get(0);
            assertNotNull(reply);
            assertThat(reply.metaData.details.statusCode, is(200));
            assertThat(reply.result, is("myPostResponse"));
            assertThat(requestMethodHolder[0], is ("POST"));
        } finally {
            serverPortPair._1.stop(0);
            commAdapter.shutdown();
        }
    }

    @Test
    void getRequestCanBeSpecified() throws Exception {
        HttpAdapter<String> commAdapter = HttpAdapter.builder(String.class).build();
        String[] requestMethodHolder = new String[1];
        Pair<com.sun.net.httpserver.HttpServer, Integer> serverPortPair =
                httpServer("/getTest", "myGetResponse", httpExchange -> requestMethodHolder[0] = httpExchange.getRequestMethod());

        try {
            TestObserver<RpcReply<String>> observer = commAdapter
                    .sendGetRequest("http://localhost:" + serverPortPair._2 + "/getTest")
                    .test();
            await().atMost(40, SECONDS).until(observer::valueCount, is(1));
            RpcReply<String> reply = observer.values().get(0);
            assertNotNull(reply);
            assertThat(reply.metaData.details.statusCode, is(200));
            assertThat(reply.result, is("myGetResponse"));
            assertThat(requestMethodHolder[0], is("GET"));
        } finally {
            serverPortPair._1.stop(0);
            commAdapter.shutdown();
        }
    }

    @Test
    void rpcWorksProperly() throws Exception {
        HttpAdapter<String> xxx = HttpAdapter.builder(String.class).build();
        Pair<com.sun.net.httpserver.HttpServer, Integer> serverPortPair =
                httpServer("/rpcTest", "rpcResponse");
        try {
            TestObserver<RpcReply<String>> observer = xxx
                    .sendRequest("http://localhost:" + serverPortPair._2 + "/rpcTest", "1", HttpRequestMethod.GET)
                    .test();

            await().atMost(40, SECONDS).until(observer::valueCount, is(1));
            observer.assertComplete();
            observer.assertValue(reply -> reply.result.equals("rpcResponse"));
        } finally {
            serverPortPair._1.stop(0);
            xxx.shutdown();
        }
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
}