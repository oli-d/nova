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
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

public class RpcServerMetricsCollector {
    private final Metrics metrics;
    private final String identifierPrefix;
    private final Meter totalNumberOfReceivedRequests;
    private final Meter totalNumberOfCompletedRequests;
    private final Meter totalNumberOfErrorRequests;

    RpcServerMetricsCollector(String identifier, Metrics metrics) {
        this.metrics = metrics;
        this.identifierPrefix = Metrics.name(identifier, "rpcServer");
        totalNumberOfReceivedRequests = metrics.getMeter(this.identifierPrefix,"requests","total");
        totalNumberOfCompletedRequests = metrics.getMeter(this.identifierPrefix,"completed","total");
        totalNumberOfErrorRequests = metrics.getMeter(this.identifierPrefix,"error","total");
    }


    public Timer.Context requestReceived (Object destination) {
        Timer specificMeter = metrics.getTimer(identifierPrefix, "requests", destination == null ? null : String.valueOf(destination));
        totalNumberOfReceivedRequests.mark();
        return specificMeter.time();
    }

    public void requestCompleted(Timer.Context context, Object reply) {
        if (context != null) {
            context.stop();
        }
        totalNumberOfCompletedRequests.mark();
    }

    public void requestCompletedExceptionally(Timer.Context context, Object destination, Throwable error) {
        if (context != null) {
            context.stop();
        }
        Meter specificMeter = metrics.getMeter(identifierPrefix, "error", destination == null ? null : String.valueOf(destination));
        specificMeter.mark();
        totalNumberOfErrorRequests.mark();
    }
}
