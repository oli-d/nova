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
import com.codahale.metrics.Gauge;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class EventMetricsCollector {
    private final Metrics metrics;
    private final String identifierPrefix;
    private final AtomicLong totalNumberOfDispatchedEvents;
    private final ConcurrentHashMap<Object,AtomicLong> eventSpecificDispatchCounters;

    public EventMetricsCollector(Metrics metrics, String identifierPrefix) {
        this.metrics = metrics;
        this.eventSpecificDispatchCounters = new ConcurrentHashMap<>();
        this.identifierPrefix = "EventBus".equalsIgnoreCase(identifierPrefix) ? identifierPrefix : Metrics.name("EventBus.", identifierPrefix);
        totalNumberOfDispatchedEvents = new AtomicLong();
        metrics.register((Gauge<Long>) totalNumberOfDispatchedEvents::get,identifierPrefix,"dispatchedEvents","total");
    }


    public void eventDispatched(Object event) {
        eventSpecificDispatchCounters.computeIfAbsent(event, key -> {
            AtomicLong dispatchCounter = new AtomicLong();
            metrics.register((Gauge<Long>) dispatchCounter::get,identifierPrefix,"dispatchedEvents",String.valueOf(key));
            return dispatchCounter;
        }).incrementAndGet();
        totalNumberOfDispatchedEvents.incrementAndGet();
//        String eventString = String.valueOf(event);
//        metrics.getMeter(identifierPrefix,"dispatchedEvents", eventString).mark();
//        metrics.getMeter(identifierPrefix,"dispatchedEvents", "total").mark();
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
