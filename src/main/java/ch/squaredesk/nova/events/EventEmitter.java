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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import ch.squaredesk.nova.events.metrics.RingBufferMetricSet;
import ch.squaredesk.nova.metrics.Metrics;
import com.codahale.metrics.MetricSet;
import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.squaredesk.nova.events.metrics.EventMetricsCollector;

import static java.util.Objects.requireNonNull;

public abstract class EventEmitter {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventEmitter.class);

	private final ConcurrentHashMap<Object, List<Emitter<Object[]>>> mapEventToHandler = new ConcurrentHashMap<>();

	private final boolean warnOnUnhandledEvents;
	protected final EventMetricsCollector metricsCollector;

	protected EventEmitter(String identifier, Metrics metrics, boolean warnOnUnhandledEvents) {
        this.warnOnUnhandledEvents = warnOnUnhandledEvents;
        this.metricsCollector = new EventMetricsCollector(metrics, identifier);
    }


    /**
     ***************************
     *                         *
     * Metrics related methods *
     *                         *
     * *************************
     */
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
     * ***********************************
     */
	abstract void dispatchEventAndData(Emitter<Object[]>[] emitters, Object event, Object... data);

	private void registerEventSpecificEmitter(Object event, Emitter<Object[]> emitter) {
	    requireNonNull(event, "event must not be null");
	    requireNonNull(emitter, "emitter must not be null");

	    // TODO: performance optimization for CurrentThreadEventEmitter
        List<Emitter<Object[]>> newList = new CopyOnWriteArrayList<>();
        List<Emitter<Object[]>> existingList = mapEventToHandler.putIfAbsent(event, newList);
        List<Emitter<Object[]>> handlers = existingList == null ? newList : existingList;
        metricsCollector.listenerAdded(event);
        handlers.add(emitter);
        LOGGER.trace("Registered listener for event {}", event);
    }

	private void deregisterEventSpecificEmitter(Object event, Emitter<Object[]> emitter) {
	    requireNonNull(event, "event must not be null");
	    requireNonNull(emitter, "emitter must not be null");

	    List<Emitter<Object[]>> handlers = mapEventToHandler.get(event);
        if (handlers==null) {
            return;
        }

        if (handlers.remove(emitter)) {
            LOGGER.trace("Registered listener for event {}", event);
            metricsCollector.listenerRemoved(event);
        }
    }

	public Observable<Object[]> observe (Object event) {
		requireNonNull(event, "event must not be null");
		Observable<Object[]> returnValue = Subject.create(s -> {
            Consumer<Object[]> listener = data -> s.onNext(data);
            registerEventSpecificEmitter(event, s);
            // deregister in case the Observable consumer unsubscribes
            s.setDisposable(new Disposable() {
                private boolean disposed = false;

                @Override
                public void dispose() {
                    if (!disposed) {
                        deregisterEventSpecificEmitter(event, s);
                        disposed = true;
                    }
                }

                @Override
                public boolean isDisposed() {
                    return false;
                }
            });
        });
		return returnValue;
	}

	public Single<Object[]> single (Object event) {
        requireNonNull(event, "event must not be null");
        Single<Object[]> returnValue = Single.create(s -> {
            // Unfortunately, for Single's, we have to work around the funny RxJava class hierarchy
            Emitter<Object[]> emitterWrapper = new Emitter<Object[]>() {
                @Override
                public void onNext(Object[] value) {
                    s.onSuccess(value);
                }

                @Override
                public void onError(Throwable error) {
                    s.onError(error);
                }

                @Override
                public void onComplete() {
                    s.onError(new RuntimeException("Unexpected onComplete() for SingleEmitter"));
                }
            };
            registerEventSpecificEmitter(event, emitterWrapper);
            // deregister in case the Observalble consumer unsubscribes
            s.setDisposable(new Disposable() {
                private boolean disposed = false;

                @Override
                public void dispose() {
                    if (!disposed) {
                        deregisterEventSpecificEmitter(event, emitterWrapper);
                        disposed = true;
                    }
                }

                @Override
                public boolean isDisposed() {
                    return false;
                }
            });
        });
        return returnValue;
	}

	// package private for testing
	Emitter<Object[]>[] getEmitters(Object event) {
		requireNonNull (event,"event must not be null");
        List<Emitter<Object[]>> eventListeners = mapEventToHandler.get(event);
        if (eventListeners!=null) {
            return eventListeners.toArray(new Emitter[eventListeners.size()]);
        } else {
            return new Emitter[0];
        }
	}

	public void emit(Object event, Object... data) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}

		Emitter<Object[]>[] emitters = getEmitters(event);
		if (emitters.length>0) {
			dispatchEventAndData(emitters, event, data);
		} else {
			metricsCollector.eventEmittedButNoListeners(event);
			if (warnOnUnhandledEvents) {
				LOGGER.warn("No listener registered for event " + event + ". Discarding dispatch with parameters "
						+ data);
			}
		}
	}

}
