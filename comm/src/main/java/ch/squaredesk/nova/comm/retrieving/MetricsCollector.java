/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.retrieving;

import ch.squaredesk.nova.metrics.Metrics;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;

import static java.util.Objects.requireNonNull;

public class MetricsCollector {
    private final Metrics metrics;
    private final String identifierPrefix;
    private final Meter totalNumberOfReceivedMessages;
    private final Counter totalNumberOfSubscriptions;
    private final Counter totalNumberOfUnparsableMessages;

    MetricsCollector(String identifier, Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        this.metrics = metrics;
        this.identifierPrefix = Metrics.name("messageReceiver", identifier);
        totalNumberOfReceivedMessages = metrics.getMeter(this.identifierPrefix,"received","total");
        totalNumberOfSubscriptions = metrics.getCounter(this.identifierPrefix,"subscriptions","total");
        totalNumberOfUnparsableMessages = metrics.getCounter(this.identifierPrefix,"unparsable","total");
    }


    public void unparsableMessageReceived(Object destination) {
        metrics.getCounter(identifierPrefix, "unparsable", String.valueOf(destination)).inc();
        totalNumberOfUnparsableMessages.inc();
    }

    public void messageReceived(Object destination) {
        metrics.getMeter(identifierPrefix, "received", String.valueOf(destination)).mark();
        totalNumberOfReceivedMessages.mark();
    }

    public void subscriptionCreated (Object destination) {
        metrics.getCounter(identifierPrefix, "subscriptions", String.valueOf(destination)).inc();
        totalNumberOfSubscriptions.inc();
    }

    public void subscriptionDestroyed (Object destination) {
        metrics.getCounter(identifierPrefix, "subscriptions", String.valueOf(destination)).dec();
        totalNumberOfSubscriptions.dec();
    }
}
