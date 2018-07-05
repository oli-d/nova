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
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public class EventBus {
    private final Logger logger = LoggerFactory.getLogger(EventBus.class);

    public final EventBusConfig eventBusConfig;

    // metrics
    private final EventMetricsCollector metricsCollector;

    // the event specific subjects
    private final ConcurrentHashMap<Object,Subject<Object[]>> eventSpecificSubjects;

    public EventBus(String identifier, EventBusConfig eventBusConfig, Metrics metrics){
            this.eventBusConfig = eventBusConfig;
            this.metricsCollector = new EventMetricsCollector(identifier, metrics);
        logger.debug("Instantiating event loop {} using the following config {}",identifier, eventBusConfig);
        eventSpecificSubjects = new ConcurrentHashMap<>();
    }

    private Subject<Object[]> getSubjectFor(Object event) {
        return eventSpecificSubjects.computeIfAbsent(event, key -> {
            metricsCollector.eventSubjectAdded(event);
            Subject<Object[]> eventSpecificSubject = PublishSubject.create();
            return eventSpecificSubject.toSerialized();
        });
    }

    public void emit (Object event, Object... data) {
        requireNonNull(event, "event must not be null");
        try {
            Subject<Object[]> subject = getSubjectFor(event);
            if (!subject.hasObservers()) {
                metricsCollector.eventEmittedButNoObservers(event);
                if (eventBusConfig.warnOnUnhandledEvents) {
                    logger.warn("Trying to dispatch event {}, but no observers could be found. Data: {}",
                            event, Arrays.toString(data));
                }
            } else {
                subject.onNext(data);
                metricsCollector.eventDispatched(event);
            }
        } catch (Exception e) {
            logger.error("Unable to emit event " + event + " with parameters " + Arrays.toString(data),e);
        }
    }

    public Flowable<Object[]> on(Object event) {
        return on(event, eventBusConfig.defaultBackpressureStrategy);
    }


    public Flowable<Object[]> on(Object event, BackpressureStrategy backpressureStrategy) {
        requireNonNull(event, "event must not be null");
        return getSubjectFor(event).toFlowable(backpressureStrategy);
    }

}
