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

	protected EventEmitter(EventMetricsCollector eventMetricsCollector, boolean warnOnUnhandledEvents) {
        this.warnOnUnhandledEvents = warnOnUnhandledEvents;
		this.metricsCollector = eventMetricsCollector;
	}


    /*
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

    /*
     *************************************
     *                                   *
     * Methods related to event handling *
     *                                   *
     * ***********************************
     */
	abstract void dispatchEventAndData(List<Emitter<Object[]>> listenerList, Object event, Object... data);

	private void registerEventSpecificEmitter(Object event, Emitter<Object[]> eventListener) {
	    requireNonNull(event, "event must not be null");
	    requireNonNull(eventListener, "eventListener must not be null");

	    // TODO: performance optimization for CurrentThreadEventEmitter
        List<Emitter<Object[]>> newList = new CopyOnWriteArrayList<>();
        List<Emitter<Object[]>> existingList = mapEventToHandler.putIfAbsent(event, newList);
        List<Emitter<Object[]>> handlers = existingList == null ? newList : existingList;
        metricsCollector.listenerAdded(event);
        handlers.add(eventListener);
        LOGGER.trace("Registered listener for event {}", event);
    }

	private void deregisterEventSpecificEmitter(Object event, Emitter<Object[]> eventListener) {
	    requireNonNull(event, "event must not be null");
	    requireNonNull(eventListener, "eventListener must not be null");

	    List<Emitter<Object[]>> handlers = mapEventToHandler.get(event);
        if (handlers==null) {
            return;
        }

        if (handlers.remove(eventListener)) {
            LOGGER.trace("Registered listener for event {}", event);
            metricsCollector.listenerRemoved(event);
        }
    }

	public Observable<Object[]> observe (Object event) {
		requireNonNull(event, "event must not be null");
		Observable<Object[]> returnValue = Subject.create(s -> {
            Consumer<Object[]> listener = data -> s.onNext(data);
            registerEventSpecificEmitter(event, s);
            // deregister in case the Observalble consumer unsubscribes
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
            // Unfortunately, for Single's, we have to ship around the funny RxJava class hierarchy
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

	protected List<Emitter<Object[]>> getListeners(Object event) {
		requireNonNull (event,"event must not be null");
		List<Emitter<Object[]>> returnValue = new ArrayList<>();
        List<Emitter<Object[]>> eventListeners = mapEventToHandler.get(event);
        if (eventListeners!=null) {
            returnValue.addAll(eventListeners);
        }
		return returnValue;
	}

	private void doEmit(Object event, Object... data) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}

		List<Emitter<Object[]>> listenerList = getListeners(event);
		if (!listenerList.isEmpty()) {
			dispatchEventAndData(listenerList, event, data);
		} else {
			metricsCollector.eventEmittedButNoListeners(event);
			if (warnOnUnhandledEvents) {
				LOGGER.warn("No listener registered for event " + event + ". Discarding dispatch with parameters "
						+ data);
			}
		}
	}

	public void emit(Object event) {
		doEmit(event);
	}

	public void emit(Object event, Object dataParam1) {
		doEmit(event, dataParam1);
	}

	public void emit(Object event, Object dataParam1, Object dataParam2) {
		doEmit(event, dataParam1, dataParam2);
	}

	public void emit(Object event, Object dataParam1, Object dataParam2, Object dataParam3) {
		doEmit(event, dataParam1, dataParam2, dataParam3);
	}

	public void emit(Object event, Object dataParam1, Object dataParam2, Object dataParam3, Object dataParam4) {
		doEmit(event, dataParam1, dataParam2, dataParam3, dataParam4);
	}

	public void emit(Object event, Object dataParam1, Object dataParam2, Object dataParam3, Object dataParam4,
			Object dataParam5) {
		doEmit(event, dataParam1, dataParam2, dataParam3, dataParam4, dataParam5);
	}

	public void emit(Object event, Object dataParam1, Object dataParam2, Object dataParam3, Object dataParam4,
			Object dataParam5, Object dataParam6) {
		doEmit(event, dataParam1, dataParam2, dataParam3, dataParam4, dataParam5, dataParam6);
	}

	public void emit(Object event, Object dataParam1, Object dataParam2, Object dataParam3, Object dataParam4,
			Object dataParam5, Object dataParam6, Object dataParam7) {
		doEmit(event, dataParam1, dataParam2, dataParam3, dataParam4, dataParam5, dataParam6, dataParam7);
	}

	public void emit(Object event, Object dataParam1, Object dataParam2, Object dataParam3, Object dataParam4,
			Object dataParam5, Object dataParam6, Object dataParam7, Object dataParam8) {
		doEmit(event, dataParam1, dataParam2, dataParam3, dataParam4, dataParam5, dataParam6, dataParam7, dataParam8);
	}

	public void emit(Object event, Object dataParam1, Object dataParam2, Object dataParam3, Object dataParam4,
			Object dataParam5, Object dataParam6, Object dataParam7, Object dataParam8, Object dataParam9) {
		doEmit(event, dataParam1, dataParam2, dataParam3, dataParam4, dataParam5, dataParam6, dataParam7, dataParam8,
				dataParam9);
	}

	public void emit(Object event, Object dataParam1, Object dataParam2, Object dataParam3, Object dataParam4,
			Object dataParam5, Object dataParam6, Object dataParam7, Object dataParam8, Object dataParam9,
			Object dataParam10) {
		doEmit(event, dataParam1, dataParam2, dataParam3, dataParam4, dataParam5, dataParam6, dataParam7, dataParam8,
				dataParam9, dataParam10);
	}
}
