/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rpc;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static ch.squaredesk.nova.metrics.MetricsName.buildName;
import static io.micrometer.core.instrument.Metrics.counter;
import static org.hamcrest.MatcherAssert.assertThat;

class RpcServerMetricsCollectorTest {
    private RpcServerMetricsCollector sut = new RpcServerMetricsCollector(null);
    private SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setup() {
        Metrics.globalRegistry.add(meterRegistry);
    }

    @AfterEach
    void destroy() {
        Metrics.globalRegistry.remove(meterRegistry);
    }

    @Test
    void requestReceived() {
        sut.requestReceived("req1");

        assertThat(counter(buildName("rpcServer.requests.total")).count(), Matchers.is(1.0));
        assertThat(counter(buildName("rpcServer.requests.req1")).count(), Matchers.is(0.0)); // count is only increased after completions of the timer
        assertThat(counter(buildName("rpcServer.completed.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.error.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.error.req1")).count(), Matchers.is(0.0));
    }

    @Test
    void requestReceivedWithNullDestinationPossible() {
        sut.requestReceived(null);

        assertThat(counter(buildName("rpcServer.requests.total")).count(), Matchers.is(1.0));
//        assertThat(metrics.getTimer("rpcServer.requests")).count(), Matchers.is(0.0)); // count is only increased after completions of the timer
        assertThat(counter(buildName("rpcServer.completed.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.completed")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.error.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.error")).count(), Matchers.is(0.0));
    }

    @Test
    void requestCompleted() {
        sut.requestCompleted(Timer.start(),"dest", "reply1");

        assertThat(counter(buildName("rpcServer.requests.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.requests.req1")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.completed.total")).count(), Matchers.is(1.0));
        assertThat(counter(buildName("rpcServer.error.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.error.req1")).count(), Matchers.is(0.0));
    }

    @Test
    void requestCompletedWithNullRequestAndReplyPossible() {
        sut.requestCompleted(null, null, null);

        assertThat(counter(buildName("rpcServer.requests.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.completed.total")).count(), Matchers.is(1.0));
        assertThat(counter(buildName("rpcServer.error.total")).count(), Matchers.is(0.0));
    }

    @Test
    void requestCompletedExceptionally() {
        sut.requestCompletedExceptionally(Timer.start(), "req3", new RuntimeException("4 test"));

        assertThat(counter(buildName("rpcServer.requests.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.requests.req3")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.completed.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.error.total")).count(), Matchers.is(1.0));
    }

    @Test
    void requestCompletedExceptionallyWithNullDestinationPossible() {
        sut.requestCompletedExceptionally(Timer.start(), null, null);

        assertThat(counter(buildName("rpcServer.requests.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.requests")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.completed.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.timeout.total")).count(), Matchers.is(0.0));
        assertThat(counter(buildName("rpcServer.error.total")).count(), Matchers.is(1.0));
    }

}