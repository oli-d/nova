/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.metrics.Metrics;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;

import static java.util.Objects.requireNonNull;

// TODO: split up in client and server part and make package private?
public class MetricsCollector {
    private final Metrics metrics;
    private final String identifierPrefix;
    private final Meter totalNumberOfReceivedMessages;
    private final Meter totalNumberOfUnparsabledMessagesReceived;
    private final Meter totalNumberOfSentMessages;
    private final Counter totalNumberOfSubscriptions;

    public MetricsCollector(Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        this.metrics = metrics;
        this.identifierPrefix = Metrics.name("websocket");
        totalNumberOfReceivedMessages = metrics.getMeter(this.identifierPrefix,"received","total");
        totalNumberOfUnparsabledMessagesReceived = metrics.getMeter(this.identifierPrefix,"received","unparsable","total");
        totalNumberOfSentMessages = metrics.getMeter(this.identifierPrefix,"sent","total");
        totalNumberOfSubscriptions = metrics.getCounter(this.identifierPrefix,"subscriptions","total");
    }


    public void messageReceived(String destination) {
        metrics.getMeter(identifierPrefix, "received", destination).mark();
        totalNumberOfReceivedMessages.mark();
    }

    public void unparsableMessageReceived(String destination) {
        metrics.getMeter(identifierPrefix, "received", "unparsable", destination).mark();
        totalNumberOfUnparsabledMessagesReceived.mark();
    }

    public void messageSent(String destination) {
        metrics.getMeter(identifierPrefix, "sent", destination).mark();
        totalNumberOfSentMessages.mark();
    }

    public void subscriptionCreated (String destination) {
        metrics.getCounter(identifierPrefix, "subscriptions", destination).inc();
        totalNumberOfSubscriptions.inc();
    }

    public void subscriptionDestroyed (String destination) {
        metrics.getCounter(identifierPrefix, "subscriptions", destination).dec();
        totalNumberOfSubscriptions.dec();
    }
}
