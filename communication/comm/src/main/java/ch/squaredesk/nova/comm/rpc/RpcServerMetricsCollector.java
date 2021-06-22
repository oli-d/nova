/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rpc;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

import static ch.squaredesk.nova.metrics.MetricsName.buildName;
import static io.micrometer.core.instrument.Metrics.counter;

public class RpcServerMetricsCollector {
    private final String identifierPrefix;
    private final Counter totalNumberOfReceivedRequests;
    private final Counter totalNumberOfCompletedRequests;
    private final Counter totalNumberOfErrorRequests;

    RpcServerMetricsCollector(String identifier) {
        this.identifierPrefix = buildName(identifier, "rpcServer");
        totalNumberOfReceivedRequests = counter(buildName(this.identifierPrefix,"requests","total"));
        totalNumberOfCompletedRequests = counter(buildName(this.identifierPrefix,"completed","total"));
        totalNumberOfErrorRequests = counter(buildName(this.identifierPrefix,"error","total"));
    }


    public void requestReceived (Object destination) {
        totalNumberOfReceivedRequests.increment();
    }

    public void requestCompleted(Timer.Sample context, Object destination, Object reply) {
        if (context != null) {
            Timer specificTimer = Metrics.timer(buildName(identifierPrefix, "requests", destination == null ? null : String.valueOf(destination)));
            context.stop(specificTimer);
        }
        totalNumberOfCompletedRequests.increment();
    }

    public void requestCompletedExceptionally(Timer.Sample context, Object destination, Throwable error) {
        if (context != null) {
            Timer specificTimer = Metrics.timer(buildName(identifierPrefix, "errors", destination == null ? null : String.valueOf(destination)));
            context.stop(specificTimer);
        }
        totalNumberOfErrorRequests.increment();
    }
}
