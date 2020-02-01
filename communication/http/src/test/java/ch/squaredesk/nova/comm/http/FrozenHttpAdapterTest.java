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

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.tuples.Pair;
import com.ning.http.client.AsyncHttpClient;
import com.sun.net.httpserver.HttpExchange;
import io.reactivex.observers.TestObserver;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("medium")
class FrozenHttpAdapterTest {
    private HttpServerSettings rsc = HttpServerSettings.builder().interfaceName("localhost").port(10000).build();
    private HttpServer httpServer = HttpServerFactory.serverFor(rsc);
    private FrozenHttpAdapter<String> sut;

    @BeforeEach
    void setup() {
        sut = HttpAdapter
                .builder()
                .setHttpServer(httpServer)
                .setHttpClient(new AsyncHttpClient())
                .build()
                .freeze(String.class);
    }

    @AfterEach
    void tearDown() {
        sut.shutdown();
    }

    @Test
    void nullDestinationReturnError() {
        TestObserver observer = sut.sendGetRequest(null).test();

        observer.assertError(t -> t instanceof IllegalArgumentException && ((IllegalArgumentException) t).getMessage().contains("Invalid URL format"));
    }

    @Test
    void invalidDestinationFormatReturnError() {
        TestObserver observer = sut.sendPostRequest("\\ßö ", "1.0").test();

        observer.assertError(t -> t instanceof IllegalArgumentException && ((IllegalArgumentException) t).getMessage().contains("Invalid URL format"));
    }

    @Test
    void notExistingDestinationReturnError() throws Exception {
        TestObserver<RpcReply<String>> observer = sut
                .sendPostRequest("http://cvf.bn.c", "")
                .test();
        observer.await(5, SECONDS);
        observer.assertError(Exception.class);
    }

    @Test
    void noReplyWithinTimeoutReturnError() throws Exception {
        Pair<com.sun.net.httpserver.HttpServer, Integer> serverPortPair =
                httpServer("/timeoutTest", "noResponse", httpExchange -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

        TestObserver<RpcReply<String>> observer = sut
                .sendGetRequest("http://localhost:" + serverPortPair._2 + "/timeoutTest", Duration.ofMillis(10))
                .test();
        observer.await(5, SECONDS);
        observer.assertError(TimeoutException.class);
    }

    @Test
    void postRequestCanBeSpecified() throws Exception {
        String[] requestMethodHolder = new String[1];
        Pair<com.sun.net.httpserver.HttpServer, Integer> serverPortPair =
                httpServer("/postTest", "myPostResponse", httpExchange -> requestMethodHolder[0] = httpExchange.getRequestMethod());

        TestObserver<RpcReply<String>> observer = sut
                .sendPostRequest("http://localhost:" + serverPortPair._2 + "/postTest", "{ myTest: \"value\"}")
                .test();
        await().atMost(40, SECONDS).until(observer::valueCount, is(1));
        RpcReply<String> reply = observer.values().get(0);
        assertNotNull(reply);
        assertThat(reply.metaData.details.statusCode, is(200));
        assertThat(reply.result, is("myPostResponse"));
        assertThat(requestMethodHolder[0], is("POST"));
    }

    @Test
    void getRequestCanBeSpecified() throws Exception {
        String[] requestMethodHolder = new String[1];
        Pair<com.sun.net.httpserver.HttpServer, Integer> serverPortPair =
                httpServer("/getTest", "myGetResponse", httpExchange -> requestMethodHolder[0] = httpExchange.getRequestMethod());

        TestObserver<RpcReply<String>> observer = sut
                .sendGetRequest("http://localhost:" + serverPortPair._2 + "/getTest")
                .test();
        await().atMost(40, SECONDS).until(observer::valueCount, is(1));
        RpcReply<String> reply = observer.values().get(0);
        assertNotNull(reply);
        assertThat(reply.metaData.details.statusCode, is(200));
        assertThat(reply.result, is("myGetResponse"));
        assertThat(requestMethodHolder[0], is("GET"));
    }

    @Test
    void rpcWorksProperly() throws Exception {
        Pair<com.sun.net.httpserver.HttpServer, Integer> serverPortPair =
                httpServer("/rpcTest", "rpcResponse");
        TestObserver<RpcReply<String>> observer = sut
                .sendRequest("http://localhost:" + serverPortPair._2 + "/rpcTest", "1", HttpRequestMethod.GET)
                .test();

        await().atMost(40, SECONDS).until(observer::valueCount, is(1));
        observer.assertComplete();
        observer.assertValue(reply -> "rpcResponse".equals(reply.result));
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