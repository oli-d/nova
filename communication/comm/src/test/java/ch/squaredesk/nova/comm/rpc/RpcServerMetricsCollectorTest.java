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

package ch.squaredesk.nova.comm.rpc;

import ch.squaredesk.nova.metrics.Metrics;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

class RpcServerMetricsCollectorTest {
    private Metrics metrics = new Metrics();
    private RpcServerMetricsCollector sut = new RpcServerMetricsCollector(null, metrics);

    @Test
    void requestReceived() {
        sut.requestReceived("req1");

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(1L));
        assertThat(metrics.getTimer("rpcServer.requests.req1").getCount(), Matchers.is(0L)); // count is only increased after completions of the timer
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.req1").getCount(), Matchers.is(0L));
    }

    @Test
    void requestReceivedWithNullDestinationPossible() {
        sut.requestReceived(null);

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(1L));
        assertThat(metrics.getTimer("rpcServer.requests").getCount(), Matchers.is(0L)); // count is only increased after completions of the timer
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error").getCount(), Matchers.is(0L));
    }

    @Test
    void requestCompleted() {
        sut.requestCompleted(metrics.getTimer("foo").time(), "reply1");

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getTimer("rpcServer.requests.req1").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.req1").getCount(), Matchers.is(0L));
    }

    @Test
    void requestCompletedWithNullRequestAndReplyPossible() {
        sut.requestCompleted(null, null);

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(0L));
    }

    @Test
    void requestCompletedExceptionally() {
        sut.requestCompletedExceptionally(metrics.getTimer("foo").time(), "req3", new RuntimeException("4 test"));

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getTimer("rpcServer.requests.req3").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(1L));
        assertThat(metrics.getMeter("rpcServer.error.req3").getCount(), Matchers.is(1L));
    }

    @Test
    void requestCompletedExceptionallyWithNullDestinationPossible() {
        sut.requestCompletedExceptionally(metrics.getTimer("foo").time(), null, null);

        assertThat(metrics.getMeter("rpcServer.requests.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getTimer("rpcServer.requests").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.completed.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.timeout.total").getCount(), Matchers.is(0L));
        assertThat(metrics.getMeter("rpcServer.error.total").getCount(), Matchers.is(1L));
    }

}