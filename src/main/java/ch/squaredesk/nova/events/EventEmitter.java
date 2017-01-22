/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.squaredesk.nova.events.metrics.EventMetricsCollector;

public abstract class EventEmitter {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventEmitter.class);

	private final HashMap<Object, List<ch.squaredesk.nova.events.EventListener>> mapEventToHandler = new HashMap<>();
	private final HashMap<Object, List<ch.squaredesk.nova.events.EventListener>> mapEventToOneOffHandlers = new HashMap<>();

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
	abstract void dispatchEventAndDataToListeners(List<ch.squaredesk.nova.events.EventListener> listenerList, Object event, Object... data);

	public void on(Object event, ch.squaredesk.nova.events.EventListener callback) {
		addListener(event, callback);
	}

	public void once(Object event, ch.squaredesk.nova.events.EventListener callback) {
		addListener(event, callback, mapEventToOneOffHandlers);
	}

	public void addListener(Object event, ch.squaredesk.nova.events.EventListener callback) {
		addListener(event, callback, mapEventToHandler);
	}

	private void addListener(Object event, ch.squaredesk.nova.events.EventListener callback, Map<Object, List<ch.squaredesk.nova.events.EventListener>> listenerMap) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		if (callback == null) {
			throw new IllegalArgumentException("handler must not be null");
		}
		List<ch.squaredesk.nova.events.EventListener> handlers = listenerMap.get(event);
		if (handlers == null) {
			handlers = new ArrayList<>();
			listenerMap.put(event, handlers);
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Registered event " + event + " --> " + callback);
		}
		metricsCollector.listenerAdded(event);
		handlers.add(callback);
	}

	public void removeListener(Object event, ch.squaredesk.nova.events.EventListener handler) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		if (handler == null) {
			throw new IllegalArgumentException("handler must not be null");
		}
		// remove listener from normal list
		List<ch.squaredesk.nova.events.EventListener> handlers = mapEventToHandler.get(event);
		if (handlers != null) {
			handlers.remove(handler);
			if (handlers.isEmpty()) {
				mapEventToHandler.remove(event);
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Deregistered listener " + event + " --> " + handler);
			}
			metricsCollector.listenerRemoved(event);
		}
		// remove listener from one off list
		handlers = mapEventToOneOffHandlers.get(event);
		if (handlers != null) {
			handlers.remove(handler);
			if (handlers.isEmpty()) {
				mapEventToHandler.remove(event);
			}
			LOGGER.trace("Deregistered one off listener " + event + " --> " + handler);
			metricsCollector.listenerRemoved(event);
		}
	}

	public void removeAllListeners(Object event) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		mapEventToHandler.remove(event);
		mapEventToOneOffHandlers.remove(event);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Deregistered all listeners for event " + event);
		}
		metricsCollector.allListenersRemoved(event);
	}

	public List<ch.squaredesk.nova.events.EventListener> getListeners(Object event) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		List<ch.squaredesk.nova.events.EventListener> returnValue = new ArrayList<>();
		List<ch.squaredesk.nova.events.EventListener> listeners = mapEventToHandler.get(event);
		if (listeners != null) {
			returnValue.addAll(listeners);
		}
		listeners = mapEventToOneOffHandlers.get(event);
		if (listeners != null) {
			returnValue.addAll(listeners);
		}

		return returnValue;
	}

	private List<ch.squaredesk.nova.events.EventListener> getListenersForEventDistribution(Object event) {
		List<ch.squaredesk.nova.events.EventListener> listenerList = new ArrayList<>();
		List<ch.squaredesk.nova.events.EventListener> normalListenerList = mapEventToHandler.get(event);
		if (normalListenerList != null) {
			listenerList.addAll(normalListenerList);
		}
		List<ch.squaredesk.nova.events.EventListener> oneOffListeners = mapEventToOneOffHandlers.remove(event);
		if (oneOffListeners != null) {
			listenerList.addAll(oneOffListeners);
		}

		return listenerList;
	}

	private void doEmit(Object event, Object... data) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}

		List<ch.squaredesk.nova.events.EventListener> listenerList = getListenersForEventDistribution(event);
		if (!listenerList.isEmpty()) {
			dispatchEventAndDataToListeners(listenerList, event, data);
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
