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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static ch.squaredesk.nova.metrics.MetricsName.buildName;
import static io.micrometer.core.instrument.Metrics.counter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RpcClientMetricsCollectorTest {
    private RpcClientMetricsCollector sut = new RpcClientMetricsCollector(null);
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
    void rpcCompleted() {
        sut.rpcCompleted("req1", "reply1");

        assertThat(counter(buildName("rpcClient","completed","total")).count(), is(1.0));
        assertThat(counter(buildName("rpcClient","completed","req1")).count(), is(1.0));

        assertThat(counter(buildName("rpcClient","timeout","total")).count(), is(0.0));
        assertThat(counter(buildName("rpcClient","timeout","req1")).count(), is(0.0));
    }

    @Test
    void rpcCompletedWithNullRequestAndReplyPossible() {
        sut.rpcCompleted("dest", null);

        assertThat(counter(buildName("rpcClient","completed","total")).count(), is(1.0));
        assertThat(counter(buildName("rpcClient","completed","dest")).count(), is(1.0));
        assertThat(counter(buildName("rpcClient","timeout","total")).count(), is(0.0));
        assertThat(counter(buildName("rpcClient","timeout","dest")).count(), is(0.0));
    }

    @Test
    void rpcTimedOut() {
        sut.rpcTimedOut("req2");

        assertThat(counter(buildName("rpcClient","completed","total")).count(), is(0.0));
        assertThat(counter(buildName("rpcClient","completed", "req2")).count(), is(0.0));

        assertThat(counter(buildName("rpcClient","timeout","total")).count(), is(1.0));
        assertThat(counter(buildName("rpcClient","timeout","req2")).count(), is(1.0));
    }

}