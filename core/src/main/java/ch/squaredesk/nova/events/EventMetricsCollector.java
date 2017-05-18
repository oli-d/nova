/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events;

import ch.squaredesk.nova.metrics.Metrics;
import com.codahale.metrics.Meter;

import java.util.concurrent.ConcurrentHashMap;

public class EventMetricsCollector {
    private final Metrics metrics;
    private final String identifierPrefix;
    private final Meter allDispatchedEvents;
    private final ConcurrentHashMap<Object,Meter> eventSpecificDispatchMeters;

    public EventMetricsCollector(Metrics metrics) {
        this (null, metrics);
    }

    public EventMetricsCollector(String identifier, Metrics metrics) {
        this.metrics = metrics;
        this.eventSpecificDispatchMeters = new ConcurrentHashMap<>();
        this.identifierPrefix = Metrics.name("eventBus", identifier);
        allDispatchedEvents = new Meter();
        metrics.register(allDispatchedEvents,this.identifierPrefix,"dispatchedEvents","total");
    }


    public void eventDispatched(Object event) {
        eventSpecificDispatchMeters.computeIfAbsent(event, key -> {
            Meter meter = new Meter();
            metrics.register(meter,identifierPrefix,"dispatchedEvents",String.valueOf(key));
            return meter;
        }).mark();
        allDispatchedEvents.mark();
    }

    public void eventSubjectAdded (Object event) {
        metrics.getCounter(identifierPrefix, "eventSubjects", "total").inc();
    }

    public void eventSubjectRemoved(Object event) {
        metrics.getCounter(identifierPrefix,"eventSubjects", "total").dec();
    }

    public void eventEmittedButNoObservers(Object event) {
        String eventString = String.valueOf(event);
        metrics.getCounter(identifierPrefix,"emitsWithNoListener", eventString).inc();
        metrics.getCounter(identifierPrefix,"emitsWithNoListener", "total").inc();
    }
}
