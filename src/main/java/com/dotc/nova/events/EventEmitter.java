package com.dotc.nova.events;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EventEmitter {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventEmitter.class);

	abstract <EventType, ParameterType> void dispatchEventAndDataToListeners(List<EventListener> listenerList, EventType event, ParameterType... data);

	private final HashMap<Object, List<EventListener>> mapEventToHandler = new HashMap<Object, List<EventListener>>();
	private final HashMap<Object, List<EventListener>> mapEventToOneOffHandlers = new HashMap<Object, List<EventListener>>();

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
				LOGGER.trace("Deregistered handler " + event + " --> " + handler);
			}
		}
		// remove listener from one off list
		handlers = mapEventToOneOffHandlers.get(event);
		if (handlers != null) {
			handlers.remove(handler);
			if (handlers.isEmpty()) {
				mapEventToHandler.remove(event);
			}
			LOGGER.trace("Deregistered one off handler " + event + " --> " + handler);
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

	public <EventType, ParameterType> void emit(EventType event, ParameterType... data) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		List<EventListener> listenerList = new ArrayList<>();
		List<EventListener> normalListenerList = mapEventToHandler.get(event);
		if (normalListenerList != null) {
			listenerList.addAll(normalListenerList);
		}
		List<EventListener> oneOffListeners = mapEventToOneOffHandlers.remove(event);
		if (oneOffListeners != null) {
			listenerList.addAll(oneOffListeners);
		}
		if (!listenerList.isEmpty()) {
			dispatchEventAndDataToListeners(listenerList, event, data);
		}
	}
}
