/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events;

import io.micrometer.core.instrument.Counter;

import java.util.concurrent.atomic.AtomicInteger;

import static ch.squaredesk.nova.metrics.MetricsName.buildName;
import static io.micrometer.core.instrument.Metrics.counter;
import static io.micrometer.core.instrument.Metrics.gauge;

public class EventMetricsCollector {
    private final String eventIdentifierPrefix;
    private final String emitsWithNoListenerIdentifierPrefix;

    private final Counter allDispatchedEvents;
    private final AtomicInteger subjectCount;
    private final Counter allEmitsWithoutListener;

    public EventMetricsCollector(String identifier) {
        this.eventIdentifierPrefix = buildName("eventBus", identifier, "dispatchedEvents");
        String eventSubjectsIdentifierPrefix = buildName("eventBus", identifier, "eventSubjects");
        this.emitsWithNoListenerIdentifierPrefix = buildName("eventBus", identifier, "emitsWithNoListener");
        allDispatchedEvents = counter(buildName(eventIdentifierPrefix, "total"));
        subjectCount = gauge(buildName(eventSubjectsIdentifierPrefix, "total"), new AtomicInteger(0));
        allEmitsWithoutListener = counter(buildName(emitsWithNoListenerIdentifierPrefix, "total"));
    }


    public void eventDispatched(Object event) {
        counter(eventIdentifierPrefix + event).increment();
        allDispatchedEvents.increment();
    }

    public void eventSubjectAdded (Object event) {
        subjectCount.incrementAndGet();
    }

    public void eventSubjectRemoved(Object event) {
        subjectCount.decrementAndGet();
    }

    public void eventEmittedButNoObservers(Object event) {
        counter(emitsWithNoListenerIdentifierPrefix + event).increment();
        allEmitsWithoutListener.increment();
    }
}
