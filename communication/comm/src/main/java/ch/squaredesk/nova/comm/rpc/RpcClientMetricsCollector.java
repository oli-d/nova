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

public class RpcClientMetricsCollector {
    private final Metrics metrics;
    private final String identifierPrefix;
    private final Meter totalNumberOfCompletedRequests;
    private final Meter totalNumberOfTimedOutRequests;

    RpcClientMetricsCollector(String identifier, Metrics metrics) {
        this.metrics = metrics;
        this.identifierPrefix = Metrics.name(identifier, "rpcClient").toString();
        totalNumberOfCompletedRequests = metrics.getMeter(this.identifierPrefix,"completed","total");
        totalNumberOfTimedOutRequests = metrics.getMeter(this.identifierPrefix,"timeout","total");
    }


    public void rpcCompleted(String destination, Object reply) {
        Meter specificMeter = metrics.getMeter( identifierPrefix, "completed", destination);
        mark(specificMeter, totalNumberOfCompletedRequests);
    }

    public void rpcTimedOut(String destination) {
        Meter specificMeter = metrics.getMeter(identifierPrefix, "timeout", destination);
        mark(specificMeter, totalNumberOfTimedOutRequests);
    }

    private void mark(Meter... meters) {
        for (Meter m : meters) {
            m.mark();
        }
    }

}
