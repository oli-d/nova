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
import com.ning.http.client.AsyncHttpClient;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.URL;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("medium")
class RpcClientTest {
    private AsyncHttpClient httpClient;
    private RpcClient sut;

    @BeforeEach
    void setup() {
        httpClient = new AsyncHttpClient();
        sut = new RpcClient("id", httpClient, new Metrics());
    }

    @AfterEach
    void tearDown() {
        sut.shutdown();
    }

    @Test
    void replyHeadersCanProperlyBeRetrieved() {
        PortFinder.withNextFreePort(port -> {
            try (SimpleHttpServer server = SimpleHttpServer.create(port, "/", httpExchange -> {
                httpExchange.getResponseHeaders().add("header1", "value1");
                httpExchange.getResponseHeaders().add("header2", "value2");
                String response = "This is the response";
                try {
                    httpExchange.sendResponseHeaders(200, response.length());
                    OutputStream os = httpExchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })) {
                RequestInfo ri = new RequestInfo(HttpRequestMethod.GET);
                RequestMessageMetaData meta = new RequestMessageMetaData(new URL("http://localhost:"+port+"/"), ri);

                Single<RpcReply<String>> replySingle = sut.sendRequest("", meta, s -> s, s -> s, 5, SECONDS);

                TestObserver<RpcReply<String>> observer = replySingle.test();
                observer.assertValueCount(1);
                observer.assertComplete();

                RpcReply<String> rpcReply = observer.values().get(0);
                assertThat(rpcReply.metaData.details.headerParams.get("header1"), is("value1"));
                assertThat(rpcReply.metaData.details.headerParams.get("header2"), is("value2"));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

}