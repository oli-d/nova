/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.retrieving;

import io.micrometer.core.instrument.Counter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.squaredesk.nova.metrics.MetricsName.buildName;
import static io.micrometer.core.instrument.Metrics.counter;
import static io.micrometer.core.instrument.Metrics.gauge;

public class MetricsCollector {
    private final String identifierPrefix;
    private final Counter totalNumberOfReceivedMessages;
    private final Map<String, AtomicInteger> subscriptionsByDestination = new ConcurrentHashMap<>();
    private final AtomicInteger totalNumberOfSubscriptions;
    private final Counter totalNumberOfUnparsableMessages;

    MetricsCollector(String identifier) {
        this.identifierPrefix = buildName(identifier, "messageReceiver");
        totalNumberOfReceivedMessages = counter(buildName(this.identifierPrefix,"received","total"));
        totalNumberOfSubscriptions = gauge(buildName(this.identifierPrefix, "subscriptions", "total"), new AtomicInteger(0));
        totalNumberOfUnparsableMessages = counter(buildName(this.identifierPrefix,"unparsable","total"));
    }


    public void unparsableMessageReceived(Object destination) {
        counter(buildName(identifierPrefix, "unparsable", String.valueOf(destination))).increment();
        totalNumberOfUnparsableMessages.increment();
    }

    public void messageReceived(Object destination) {
        counter(buildName(identifierPrefix, "received", String.valueOf(destination))).increment();
        totalNumberOfReceivedMessages.increment();
    }

    public void subscriptionCreated (Object destination) {
        String gaugeName = buildName(identifierPrefix, "subscriptions", String.valueOf(destination));
        AtomicInteger specificCount = subscriptionsByDestination.computeIfAbsent(
                gaugeName,
                key -> gauge(gaugeName, new AtomicInteger(0))
        );
        specificCount.incrementAndGet();
        totalNumberOfSubscriptions.incrementAndGet();
    }

    public void subscriptionDestroyed (Object destination) {
        totalNumberOfSubscriptions.decrementAndGet();
        String gaugeName = buildName(identifierPrefix, "subscriptions", String.valueOf(destination));
        AtomicInteger specificCount = subscriptionsByDestination.get(gaugeName);
        if (specificCount == null) {
            // WTF?!?!
            return;
        }
        specificCount.decrementAndGet();
    }
}
