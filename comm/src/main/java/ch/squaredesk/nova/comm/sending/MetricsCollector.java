/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.sending;

import ch.squaredesk.nova.metrics.Metrics;
import com.codahale.metrics.Meter;

import java.util.Objects;

class MetricsCollector {
    private final Metrics metrics;
    private final String identifierPrefix;
    private final Meter totalNumberOfSentMessages;

    MetricsCollector(String identifier, Metrics metrics) {
        Objects.requireNonNull(metrics, "metrics must not be null");
        this.metrics = metrics;
        this.identifierPrefix = Metrics.name("messageSender", identifier);
        totalNumberOfSentMessages = metrics.getMeter(this.identifierPrefix,"sent","total");
    }


    public void messageSent(Object destination) {
        metrics.getMeter(identifierPrefix, "sent", String.valueOf(destination)).mark();
        totalNumberOfSentMessages.mark();
    }

}
