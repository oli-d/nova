/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rpc;

import ch.squaredesk.nova.metrics.MetricsName;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

public class RpcClientMetricsCollector {
    private final String identifierPrefix;
    private final Counter totalNumberOfCompletedRequests;
    private final Counter totalNumberOfTimedOutRequests;

    RpcClientMetricsCollector(String identifier) {
        this.identifierPrefix = MetricsName.buildName(identifier, "rpcClient");
        totalNumberOfCompletedRequests = Metrics.counter(MetricsName.buildName(this.identifierPrefix,"completed","total"));
        totalNumberOfTimedOutRequests = Metrics.counter(MetricsName.buildName(this.identifierPrefix,"timeout","total"));
    }


    public void rpcCompleted(String destination, Object reply) {
        Metrics.counter(MetricsName.buildName( identifierPrefix, "completed", destination)).increment();
        totalNumberOfCompletedRequests.increment();
    }

    public void rpcTimedOut(String destination) {
        Metrics.counter(MetricsName.buildName( identifierPrefix, "timeout", destination)).increment();
        totalNumberOfTimedOutRequests.increment();
    }
}
