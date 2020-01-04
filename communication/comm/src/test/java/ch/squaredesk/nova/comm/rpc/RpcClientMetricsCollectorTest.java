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
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RpcClientMetricsCollectorTest {
    private Metrics metrics = new Metrics();
    private RpcClientMetricsCollector sut = new RpcClientMetricsCollector(null, metrics);

    @Test
    void rpcCompleted() {
        sut.rpcCompleted("req1", "reply1");

        assertThat(metrics.getMeter("rpcClient","completed","total").getCount(), is(1L));
        assertThat(metrics.getMeter("rpcClient","completed","req1").getCount(), is(1L));

        assertThat(metrics.getMeter("rpcClient","timeout","total").getCount(), is(0L));
        assertThat(metrics.getMeter("rpcClient","timeout","req1").getCount(), is(0L));
    }

    @Test
    void rpcCompletedWithNullRequestAndReplyPossible() {
        sut.rpcCompleted("dest", null);

        assertThat(metrics.getMeter("rpcClient","completed","total").getCount(), is(1L));
        assertThat(metrics.getMeter("rpcClient","completed","dest").getCount(), is(1L));
        assertThat(metrics.getMeter("rpcClient","timeout","total").getCount(), is(0L));
        assertThat(metrics.getMeter("rpcClient","timeout","dest").getCount(), is(0L));
    }

    @Test
    void rpcTimedOut() {
        sut.rpcTimedOut("req2");

        assertThat(metrics.getMeter("rpcClient","completed","total").getCount(), is(0L));
        assertThat(metrics.getMeter("rpcClient","completed", "req2").getCount(), is(0L));

        assertThat(metrics.getMeter("rpcClient","timeout","total").getCount(), is(1L));
        assertThat(metrics.getMeter("rpcClient","timeout","req2").getCount(), is(1L));
    }

}