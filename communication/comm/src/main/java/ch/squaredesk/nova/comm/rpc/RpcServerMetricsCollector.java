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
import io.dropwizard.metrics5.Meter;

public class RpcServerMetricsCollector {
    private final Metrics metrics;
    private final String identifierPrefix;
    private final Meter totalNumberOfReceivedRequests;
    private final Meter totalNumberOfCompletedRequests;
    private final Meter totalNumberOfErrorRequests;

    RpcServerMetricsCollector(String identifier, Metrics metrics) {
        this.metrics = metrics;
        this.identifierPrefix = Metrics.name(identifier, "rpcServer").toString();
        totalNumberOfReceivedRequests = metrics.getMeter(this.identifierPrefix,"requests","total");
        totalNumberOfCompletedRequests = metrics.getMeter(this.identifierPrefix,"completed","total");
        totalNumberOfErrorRequests = metrics.getMeter(this.identifierPrefix,"error","total");
    }


    public void requestReceived (Object destination) {
        Meter specificMeter = metrics.getMeter(identifierPrefix, "requests", String.valueOf(destination));
        mark(specificMeter,totalNumberOfReceivedRequests);
    }

    public void requestCompleted(Object destination, Object reply) {
        Meter specificMeter = metrics.getMeter(identifierPrefix, "completed", String.valueOf(destination));
        mark(specificMeter, totalNumberOfCompletedRequests);
    }

    public void requestCompletedExceptionally(Object destination, Throwable error) {
        Meter specificMeter = metrics.getMeter(identifierPrefix, "error", String.valueOf(destination));
        mark(specificMeter, totalNumberOfErrorRequests);
    }

    private void mark(Meter... meters) {
        for (Meter m : meters) {
            m.mark();
        }
    }


}
