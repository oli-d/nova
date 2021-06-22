/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;

public class EventBus {
    private final Logger logger = LoggerFactory.getLogger(EventBus.class);

    private final boolean warnOnUnhandledEvents;
    private final Scheduler defaultScheduler;
    private final BackpressureStrategy defaultBackpressureStrategy;
    private final EventMetricsCollector metricsCollector;
    private final ConcurrentHashMap<Object, Subject<Object[]>> eventSpecificSubjects;

    public EventBus(String identifier,
                    EventDispatchConfig eventDispatchConfig){
        logger.debug(
                "Instantiating event loop {} using {}",
                identifier,
                eventDispatchConfig);
        this.defaultBackpressureStrategy = eventDispatchConfig.defaultBackpressureStrategy();
        this.warnOnUnhandledEvents = eventDispatchConfig.warnOnUnhandledEvents();
        this.metricsCollector = new EventMetricsCollector(identifier);
        this.eventSpecificSubjects = new ConcurrentHashMap<>();
        this.defaultScheduler = EventBus.createDefaultSchedulerFrom(eventDispatchConfig);
    }

    private static Scheduler createDefaultSchedulerFrom (EventDispatchConfig config) {
        switch (config.eventDispatchMode()) {
            case BLOCKING:
                // nothing to do
                return null;
            case NON_BLOCKING_FIXED_THREAD_POOL:
                ThreadFactory threadFactoryBizLogic = runnable -> {
                    Thread t = new Thread(runnable, "NovaEventDispatcher");
                    t.setDaemon(true);
                    return t;
                };
                if (config.parallelism() > 1) {
                    return Schedulers.from(Executors.newFixedThreadPool(config.parallelism(), threadFactoryBizLogic));
                } else {
                    return Schedulers.from(Executors.newSingleThreadExecutor(threadFactoryBizLogic));
                }
            case NON_BLOCKING_WORK_STEALING_POOL:
                if (config.parallelism() == 0) {
                    return Schedulers.from(Executors.newWorkStealingPool());
                } else {
                    return Schedulers.from(Executors.newWorkStealingPool(config.parallelism()));
                }
        }
        throw new RuntimeException("Unsupported EventDispatchMode " + config.eventDispatchMode());
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
                if (warnOnUnhandledEvents) {
                    logger.warn("Trying to dispatch event {}, but no observers could be found. Data: {}",
                            event, Arrays.toString(data));
                }
            } else {
                subject.onNext(data);
                metricsCollector.eventDispatched(event);
            }
        } catch (Exception e) {
            logger.error("Unable to emit event {} with parameters {}", event, Arrays.toString(data),e);
        }
    }

    public Flowable<Object[]> on(Object event) {
        return on(event, defaultBackpressureStrategy, defaultScheduler);
    }

    public Flowable<Object[]> on(Object event, BackpressureStrategy backpressureStrategy) {
        return on(event, backpressureStrategy, defaultScheduler);
    }


    public Flowable<Object[]> on(Object event, BackpressureStrategy backpressureStrategy, Scheduler scheduler) {
        Flowable<Object[]> flowable =
                getSubjectFor(requireNonNull(event, "event must not be null"))
                .toFlowable(Optional.ofNullable(backpressureStrategy).orElse(defaultBackpressureStrategy));

        if (scheduler != null) {
            flowable = flowable.observeOn(scheduler);
        }

        return flowable;
    }

}
