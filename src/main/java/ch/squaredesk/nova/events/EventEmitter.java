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
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public abstract class EventEmitter {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventEmitter.class);

	private final ConcurrentHashMap<Object, Consumer<Object[]>> mapEventToConsumer = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Object, Observable<Object[]>> mapEventToObservable = new ConcurrentHashMap<>();

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
	abstract void dispatchEventAndData(Consumer<Object[]> consumer, Object event, Object... data);

	private void registerEventConsumer(Object event, Consumer<Object[]> consumer) {
	    requireNonNull(event, "event must not be null");
	    requireNonNull(consumer, "consumer must not be null");

        if (mapEventToConsumer.put(event, consumer) != null) {
            throw new IllegalStateException("registered a second consumer for event " + event);
        }
        metricsCollector.listenerAdded(event);
        LOGGER.trace("Registered listener for event {}", event);
    }

	private void deregisterEventConsumer(Object event) {
	    requireNonNull(event, "event must not be null");
        mapEventToConsumer.remove(event);
        mapEventToObservable.remove(event);
        metricsCollector.listenerRemoved(event);
        LOGGER.trace("Removed listener for event {}", event);
    }

	public Observable<Object[]> observe (Object event) {
		requireNonNull(event, "event must not be null");
		Observable<Object[]> retVal = mapEventToObservable.computeIfAbsent(event, x -> {
            Observable<Object[]> inner = Observable.create(s -> {
                s.setDisposable(new Disposable() {
                    private boolean disposed = false;

                    @Override
                    public void dispose() {
                        if (!disposed) {
                            deregisterEventConsumer(event);
                            disposed = true;
                        }
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                });
                registerEventConsumer(x, data -> s.onNext(data));
            });
            return inner.publish().refCount();
        });

        return retVal;
	}

	public Single<Object[]> single (Object event) {
        return observe(event).first(new Object[0]);
	}

	// package private for testing
    Consumer<Object[]> consumerFor(Object event) {
		requireNonNull (event,"event must not be null");
        return mapEventToConsumer.get(event);
	}

	public void emit(Object event, Object... data) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}

        Consumer<Object[]> consumer = consumerFor(event);
		if (consumer!=null) {
			dispatchEventAndData(consumer, event, data);
		} else {
			metricsCollector.eventEmittedButNoListeners(event);
			if (warnOnUnhandledEvents) {
				LOGGER.warn("No listener registered for event " + event + ". Discarding dispatch with parameters "
						+ data);
			}
		}
	}

}
