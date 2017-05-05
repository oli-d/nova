/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rpc;

import ch.squaredesk.nova.metrics.Metrics;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

class RpcServerMetricsCollectorTest {
    private Metrics metrics = new Metrics();
    private RpcServerMetricsCollector sut = new RpcServerMetricsCollector(null, metrics);

    @Test
    void requestReceived() throws Exception {
        sut.requestReceived("req1");

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.requests.String").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.String").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.timeout.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.timeout.String").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.String").getCount(), Matchers.is(0L));
    }

    @Test
    void requestReceivedWithNullRequestPossible() throws Exception {
        sut.requestReceived(null);

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.requests.null").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.null").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.timeout.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.timeout.null").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.null").getCount(), Matchers.is(0L));
    }

    @Test
    void requestCompleted() throws Exception {
        sut.requestCompleted("req1", "reply1");

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.requests.String").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.completed.String").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.timeout.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.timeout.String").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.String").getCount(), Matchers.is(0L));
    }

    @Test
    void requestCompletedWithNullRequestAndReplyPossible() throws Exception {
        sut.requestCompleted(null, null);

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.requests.null").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.completed.null").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.timeout.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.timeout.String").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.String").getCount(), Matchers.is(0L));
    }

    @Test
    void requestCompletedExceptionally() throws Exception {
        sut.requestCompletedExceptionally("req3", new RuntimeException("4 test"));

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.requests.String").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.String").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.timeout.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.timeout.String").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.error.String").getCount(), Matchers.is(1L));
    }

    @Test
    void requestCompletedExceptionallyWithNullRequestPossible() throws Exception {
        sut.requestCompletedExceptionally(null, null);

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.requests.null").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.null").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.timeout.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.timeout.null").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.error.null").getCount(), Matchers.is(1L));
    }

}