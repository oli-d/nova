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
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RpcClientMetricsCollectorTest {
    private Metrics metrics = new Metrics();
    private RpcClientMetricsCollector sut = new RpcClientMetricsCollector(null, metrics);

    @Test
    void rpcCompleted() throws Exception {
        sut.rpcCompleted("req1", "reply1");

        assertThat(metrics.getMeter("rpcClient","completed","total").getCount(), is(1L));
        assertThat(metrics.getMeter("rpcClient","completed","String").getCount(), is(1L));

        assertThat(metrics.getMeter("rpcClient","timeout","total").getCount(), is(0L));
        assertThat(metrics.getMeter("rpcClient","timeout","String").getCount(), is(0L));
    }

    @Test
    void rpcCompletedWithNullRequestAndReplyPossible() throws Exception {
        sut.rpcCompleted(null, null);

        assertThat(metrics.getMeter("rpcClient","completed","total").getCount(), is(1L));
        assertThat(metrics.getMeter("rpcClient","completed","null").getCount(), is(1L));
        assertThat(metrics.getMeter("rpcClient","timeout","total").getCount(), is(0L));
        assertThat(metrics.getMeter("rpcClient","timeout","null").getCount(), is(0L));
    }

    @Test
    void rpcTimedOut() throws Exception {
        sut.rpcTimedOut("req2");

        assertThat(metrics.getMeter("rpcClient","completed","total").getCount(), is(0L));
        assertThat(metrics.getMeter("rpcClient","completed","String").getCount(), is(0L));

        assertThat(metrics.getMeter("rpcClient","timeout","total").getCount(), is(1L));
        assertThat(metrics.getMeter("rpcClient","timeout","String").getCount(), is(1L));
    }

}