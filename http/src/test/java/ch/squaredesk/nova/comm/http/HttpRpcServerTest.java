/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.rpc.RpcInvocation;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.Disposable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class HttpRpcServerTest {
    private HttpRpcServer<String> sut;
    private int port;

    @BeforeEach
    void setup() throws Exception {
        port = HttpHelper.nextFreePort();
        sut = new HttpRpcServer<>(
                "RpcServerTest",
                port,
                s -> s,
                s -> s,
                t -> "Error: " + t.getMessage(),
                new Metrics());
        sut.start();
        HttpHelper.waitUntilSomebodyListensOnPort(port, 500, TimeUnit.MILLISECONDS);
    }

    @AfterEach
    void tearDown() {
        sut.shutdown();
    }

    @Test
    void subscriberProperlyInvoked() throws Exception {
        List<RpcInvocation<String, String, HttpSpecificInfo>> invocations1 = new ArrayList<>();
        List<RpcInvocation<String, String, HttpSpecificInfo>> invocations2 = new ArrayList<>();
        List<RpcInvocation<String, String, HttpSpecificInfo>> invocations3 = new ArrayList<>();
        Disposable d1 = sut.requests("/1", BackpressureStrategy.BUFFER).subscribe(
                invocation -> {
                    invocations1.add(invocation);
                    invocation.complete("reply1");
                }
        );
        Disposable d2 = sut.requests("/2", BackpressureStrategy.BUFFER).subscribe(
                invocation -> {
                    invocations2.add(invocation);
                    invocation.complete("reply2");
                }
        );
        Disposable d3 = sut.requests("/3", BackpressureStrategy.BUFFER).subscribe(
                invocation -> {
                    invocations3.add(invocation);
                    invocation.complete("reply3");
                }
        );

        HttpHelper.getResponseBody("http://0.0.0.0:"+port+"/1", "request");
        HttpHelper.getResponseBody("http://0.0.0.0:"+port+"/2", "request");
        HttpHelper.getResponseBody("http://0.0.0.0:" + port + "/4", "request");

        assertThat(invocations1.size(), is(1));
        assertThat(invocations2.size(), is(1));
        assertThat(invocations3.size(), is(0));

        // be nice and clean up
        d1.dispose();
        d2.dispose();
        d3.dispose();
    }

    @Test
    void rpcCompletionProperlyTransmittedToRequestor() throws Exception {
        sut.requests("/echo", BackpressureStrategy.BUFFER).subscribe(
                invocation -> invocation.complete(invocation.request));

        String request = UUID.randomUUID().toString();
        String reply = HttpHelper.getResponseBody("http://0.0.0.0:" + port + "/echo", request);

        assertThat(reply, is(request));
    }

    @Test
    void rpcCompletionExceptionProperlyTransmittedToRequestor() throws Exception {
        sut.requests("/alwaysError", BackpressureStrategy.BUFFER).subscribe(
                invocation -> invocation.completeExceptionally(new RuntimeException(invocation.request)));

        String request = UUID.randomUUID().toString();
        String reply = HttpHelper.getResponseBody("http://0.0.0.0:" + port + "/alwaysError", request);

        assertThat(reply, is("Error: " + request)); // according to the ErrorMessageCreator, the sut is setup with
    }
}