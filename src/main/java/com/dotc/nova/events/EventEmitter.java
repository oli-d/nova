package com.dotc.nova.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dotc.nova.events.metrics.EventMetricsCollector;

public abstract class EventEmitter {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventEmitter.class);

	private final HashMap<Object, List<EventListener>> mapEventToHandler = new HashMap<>();
	private final HashMap<Object, List<EventListener>> mapEventToOneOffHandlers = new HashMap<>();

	private final boolean warnOnUnhandledEvents;
	protected final EventMetricsCollector metricsCollector;

	protected EventEmitter(boolean warnOnUnhandledEvents, EventMetricsCollector eventMetricsCollector) {
		this.warnOnUnhandledEvents = warnOnUnhandledEvents;
		this.metricsCollector = eventMetricsCollector;
	}

	abstract void dispatchEventAndDataToListeners(List<EventListener> listenerList, Object event, Object... data);

	public void on(Object event, EventListener callback) {
		addListener(event, callback);
	}

	public void once(Object event, EventListener callback) {
		addListener(event, callback, mapEventToOneOffHandlers);
	}

	public void addListener(Object event, EventListener callback) {
		addListener(event, callback, mapEventToHandler);
	}

	private void addListener(Object event, EventListener callback, Map<Object, List<EventListener>> listenerMap) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		if (callback == null) {
			throw new IllegalArgumentException("handler must not be null");
		}
		List<EventListener> handlers = listenerMap.get(event);
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

	public void removeListener(Object event, EventListener handler) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		if (handler == null) {
			throw new IllegalArgumentException("handler must not be null");
		}
		// remove listener from normal list
		List<EventListener> handlers = mapEventToHandler.get(event);
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

	public List<EventListener> getListeners(Object event) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		List<EventListener> returnValue = new ArrayList<>();
		List<EventListener> listeners = mapEventToHandler.get(event);
		if (listeners != null) {
			returnValue.addAll(listeners);
		}
		listeners = mapEventToOneOffHandlers.get(event);
		if (listeners != null) {
			returnValue.addAll(listeners);
		}

		return returnValue;
	}

	private List<EventListener> getListenersForEventDistribution(Object event) {
		List<EventListener> listenerList = new ArrayList<>();
		List<EventListener> normalListenerList = mapEventToHandler.get(event);
		if (normalListenerList != null) {
			listenerList.addAll(normalListenerList);
		}
		List<EventListener> oneOffListeners = mapEventToOneOffHandlers.remove(event);
		if (oneOffListeners != null) {
			listenerList.addAll(oneOffListeners);
		}

		return listenerList;
	}

	private void doEmit(Object event, Object... data) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}

		List<EventListener> listenerList = getListenersForEventDistribution(event);
		if (!listenerList.isEmpty()) {
			dispatchEventAndDataToListeners(listenerList, event, data);
		} else {
			metricsCollector.eventEmittedButNoListeners(event);
			if (warnOnUnhandledEvents) {
				LOGGER.warn("No listener registered for event " + event + ". Discarding dispatch with parameters " + data);
			}
		}
	}

	public <EventType, ParamType> void emit(EventType event, ParamType data) {
		doEmit(event, data);
	}

	public <EventType, Param1Type, Param2Type> void emit(EventType event, Param1Type data1, Param2Type data2) {
		doEmit(event, data1, data2);
	}

	public <EventType, Param1Type, Param2Type, Param3Type> void emit(EventType event, Param1Type data1, Param2Type data2, Param3Type data3) {
		doEmit(event, data1, data2, data3);
	}

	public <EventType, Param1Type, Param2Type, Param3Type, Param4Type> void emit(EventType event, Param1Type data1, Param2Type data2,
			Param3Type data3, Param4Type data4) {
		doEmit(event, data1, data2, data3, data4);
	}

	public <EventType, Param1Type, Param2Type, Param3Type, Param4Type, Param5Type> void emit(EventType event, Param1Type data1,
			Param2Type data2, Param3Type data3, Param4Type data4, Param5Type data5) {
		doEmit(event, data1, data2, data3, data4, data5);
	}

	public <EventType, Param1Type, Param2Type, Param3Type, Param4Type, Param5Type, Param6Type> void emit(EventType event, Param1Type data1,
			Param2Type data2, Param3Type data3, Param4Type data4, Param5Type data5, Param6Type data6) {
		doEmit(event, data1, data2, data3, data4, data5, data6);
	}

	public <EventType, Param1Type, Param2Type, Param3Type, Param4Type, Param5Type, Param6Type, Param7Type> void emit(EventType event,
			Param1Type data1, Param2Type data2, Param3Type data3, Param4Type data4, Param5Type data5, Param6Type data6, Param7Type data7) {
		doEmit(event, data1, data2, data3, data4, data5, data6, data7);
	}

	public <EventType, Param1Type, Param2Type, Param3Type, Param4Type, Param5Type, Param6Type, Param7Type, Param8Type> void emit(
			EventType event, Param1Type data1, Param2Type data2, Param3Type data3, Param4Type data4, Param5Type data5, Param6Type data6,
			Param7Type data7, Param8Type data8) {
		doEmit(event, data1, data2, data3, data4, data5, data6, data7, data8);
	}

	public <EventType, Param1Type, Param2Type, Param3Type, Param4Type, Param5Type, Param6Type, Param7Type, Param8Type, Param9Type> void emit(
			EventType event, Param1Type data1, Param2Type data2, Param3Type data3, Param4Type data4, Param5Type data5, Param6Type data6,
			Param7Type data7, Param8Type data8, Param9Type data9) {
		doEmit(event, data1, data2, data3, data4, data5, data6, data7, data8, data9);
	}

	public <EventType, Param1Type, Param2Type, Param3Type, Param4Type, Param5Type, Param6Type, Param7Type, Param8Type, Param9Type, Param10Type> void emit(
			EventType event, Param1Type data1, Param2Type data2, Param3Type data3, Param4Type data4, Param5Type data5, Param6Type data6,
			Param7Type data7, Param8Type data8, Param9Type data9, Param10Type data10) {
		doEmit(event, data1, data2, data3, data4, data5, data6, data7, data8, data9, data10);
	}

	public void enableMetricsTrackingFor(Object... events) {
		metricsCollector.setTrackingEnabled(true, events);
	}

	public void disableMetricsTrackingFor(Object... events) {
		metricsCollector.setTrackingEnabled(false, events);
	}

}
