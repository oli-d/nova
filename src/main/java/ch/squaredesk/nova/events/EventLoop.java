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

import ch.squaredesk.nova.events.metrics.EventMetricsCollector;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;

public class EventLoop {
	private final Logger logger = LoggerFactory.getLogger(EventLoop.class);

	private final EventMetricsCollector metricsCollector;

    // the source of all events
    private final Subject<InvocationContext> theSource = PublishSubject.create();
    private final ConcurrentHashMap<Object,Subject<Object[]>> eventSpecificSubjects = new ConcurrentHashMap<>();
    private final EventLoopConfig eventLoopConfig;

    public EventLoop(String identifier, EventLoopConfig eventLoopConfig, Metrics metrics) {
        this.eventLoopConfig = eventLoopConfig;
        this.metricsCollector = new EventMetricsCollector(metrics, identifier);

        if (logger.isDebugEnabled()) {
            logger.debug("Instantiating event loop " + identifier + ", using the following configuration:");
            logger.debug("\tDispatching in emitter thread: " + eventLoopConfig.dispatchInEmitterThread);
            logger.debug("\tdefaultBackpressureStrategy:   " + eventLoopConfig.defaultBackpressureStrategy);
            logger.debug("\twarn on unhandled events:      " + eventLoopConfig.warnOnUnhandledEvent);
        }

        Observable<InvocationContext> threadedSource = theSource;
        if (!eventLoopConfig.dispatchInEmitterThread) {
            Executor dispatchExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "EventLoop/" + identifier);
                t.setDaemon(true);
                return t;
            });

            threadedSource = theSource.observeOn(Schedulers.from(dispatchExecutor));
        }

        Disposable sourceDisposable = threadedSource
                .subscribe(invocationContext -> {
                    try {
                        Subject<Object[]> s = eventSpecificSubjects.get(invocationContext.event);
                        if (s!=null) {
                            if (s.hasObservers()) {
                                metricsCollector.eventDispatched(invocationContext.event);
                                s.onNext(invocationContext.data);
                            } else {
                                logger.debug("No observers for event {}, disposing subject.", invocationContext.event);
                                eventSpecificSubjects.remove(invocationContext.event);
                                metricsCollector.eventEmittedButNoListeners(invocationContext.event);
                                if (eventLoopConfig.warnOnUnhandledEvent) {
                                    logger.warn("No listener registered for event " + invocationContext.event
                                            + ". Discarding dispatch with parameters " + Arrays.toString(invocationContext.data));
                                }
                            }
                        } else {
                            metricsCollector.eventEmittedButNoListeners(invocationContext.event);
                            logger.warn("No listener registered for event " + invocationContext.event
                                    + ". Discarding dispatch with parameters " + Arrays.toString(invocationContext.data));
                        }
                    } catch (Exception e) {
                        logger.error("An error occurred, trying to dispatch event " + invocationContext, e);
                    }
                });
    }

    /**
     ***************************
     *                         *
     * Metrics related methods *
     *                         *
     ***************************
     **/
    public void enableMetricsTrackingFor(Object... events) {
        if (events != null && events.length > 0) {
            Arrays.stream(events).forEach(event -> {
                metricsCollector.setTrackingEnabled(true, event);
            });
        }
    }

    public void disableMetricsTrackingFor(Object... events) {
        if (events != null && events.length > 0) {
            Arrays.stream(events).forEach(event -> {
                metricsCollector.setTrackingEnabled(false, event);
            });
        }
    }

    /**
     *************************************
     *                                   *
     * Methods related to event handling *
     *                                   *
     *************************************
     **/
    public void emit (Object event, Object... data) {
        requireNonNull(event, "event must not be null");
        theSource.onNext(new InvocationContext(event, data));
    }

	public Flowable<Object[]> on(Object event) {
		requireNonNull(event, "event must not be null");
        Subject<Object[]> eventSpecificSubject = eventSpecificSubjects.computeIfAbsent(event, key -> {
            PublishSubject<Object[]> ps = PublishSubject.create();
            return ps;
        });
        return eventSpecificSubject.toFlowable(eventLoopConfig.defaultBackpressureStrategy).doFinally(() -> {
            if (!eventSpecificSubject.hasObservers()) {
                logger.info("No observers left for event {}, nuking subject...",event);
                eventSpecificSubject.onComplete();
                eventSpecificSubjects.remove(event);
            }
        });
	}

	public Single<Object[]> single (Object event) {
        return on(event)
                .first(new Object[0]);
	}

	// package private for testing
    Subject<Object[]> subjectFor(Object event) {
		requireNonNull (event,"event must not be null");
        return eventSpecificSubjects.get(event);
	}

}
