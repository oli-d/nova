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
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.sun.net.httpserver.HttpExchange;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Tag("medium")
class RpcClientTest {
    private AsyncHttpClient httpClient;
    private RpcClient sut;

    @BeforeEach
    void setup() {
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setRequestTimeout((int)SECONDS.toMillis(2)).build();
        httpClient = new AsyncHttpClient(config);
        sut = new RpcClient("id", httpClient, new Metrics());
    }

    @AfterEach
    void tearDown() {
        sut.shutdown();
    }

    @Test
    void replyHeadersCanProperlyBeRetrieved() throws Exception {
        Pair<Integer, SimpleHttpServer> portServerPair = createHttpServer(httpExchange -> {
            String response = "This is the response";
            try {
                httpExchange.getResponseHeaders().add("header1", "value1");
                httpExchange.getResponseHeaders().add("header2", "value2");
                try {
                    httpExchange.sendResponseHeaders(200, response.length());
                    OutputStream os = httpExchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).blockingGet();

        try {
            RequestInfo ri = new RequestInfo(HttpRequestMethod.GET);
            RequestMessageMetaData meta = new RequestMessageMetaData(new URL("http://localhost:" + portServerPair._1 + "/"), ri);

            Single<RpcReply<String>> replySingle = sut.sendRequest("", meta, s -> s, s -> s, 5, SECONDS);

            TestObserver<RpcReply<String>> observer = replySingle.test();
            await().atMost(3, SECONDS).until(observer::valueCount, is(1));
            observer.assertComplete();

            RpcReply<String> rpcReply = observer.values().get(0);
            assertThat(rpcReply.metaData.details.headerParams.get("header1"), is("value1"));
            assertThat(rpcReply.metaData.details.headerParams.get("header2"), is("value2"));
        } finally {
            portServerPair._2.close();
        }
    }

    @Test
    private Single<Pair<Integer, SimpleHttpServer>> createHttpServer(Consumer<HttpExchange> httpExchangeConsumer) {
        return Single.fromCallable(() -> {
            int[] ports = new int[1];
            SimpleHttpServer[] servers = new SimpleHttpServer[1];

            PortFinder.withNextFreePort(port -> {
                try {
                    ports[0] = port;
                    servers[0] = SimpleHttpServer.create(port, "/", httpExchangeConsumer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return new Pair<> (ports[0], servers[0]);
        }).subscribeOn(Schedulers.newThread());

    }

    @Test
    void timeoutIsProperlyAppliedWhenDoingRpc() throws Exception {
        Pair<Integer, SimpleHttpServer> portServerPair = createHttpServer(httpExchange -> {
            // we just will never come back to force the timeout
        }).blockingGet();

        try {
            RequestInfo ri = new RequestInfo(HttpRequestMethod.GET);
            RequestMessageMetaData meta = new RequestMessageMetaData(new URL("http://localhost:"+portServerPair._1+"/"), ri);

            Single<RpcReply<String>> replySingle = sut.sendRequest("", meta, s -> s, s -> s, 5, SECONDS);

            TestObserver<RpcReply<String>> observer = replySingle.test();

            // sleep 1 second and validate that the request is being processed
            Thread.sleep(1000);
            observer.assertNotComplete();
            observer.assertValueCount(0);

            // sleep 2 more seconds and validate that the request is still being processed (more than httpClient's default timeout)
            Thread.sleep(2000);
            observer.assertNotComplete();
            observer.assertValueCount(0);

            // sleep 4 more seconds and validate that we ran into a timeout in the meantime
            Thread.sleep(5000);
            observer.assertValueCount(0);
            observer.assertError(error -> error instanceof TimeoutException);
        } finally {
            portServerPair._2.close();
        }
    }

}