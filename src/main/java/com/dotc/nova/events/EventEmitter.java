package com.dotc.nova.events;

import java.util.*;

import org.apache.log4j.Logger;

import com.dotc.nova.dispatching.EventDispatcher;

public class EventEmitter {
	private static final Logger LOGGER = Logger.getLogger(EventEmitter.class);

	private final HashMap<Class, List<EventListener>> mapTypeToListener = new HashMap<Class, List<EventListener>>();

	private final EventDispatcher eventDispatcher;

	public EventEmitter(EventDispatcher eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
	}

	public <T> void on(Class<T> type, EventListener<T> listener) {
		addListener(type, listener);
	}

	public <T> void addListener(Class<T> type, EventListener<T> listener) {
		if (type == null) {
			throw new IllegalArgumentException("type must not be null");
		}
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null");
		}
		List<EventListener> handlers = mapTypeToListener.get(type);
		if (handlers == null) {
			handlers = new ArrayList<EventListener>();
			mapTypeToListener.put(type, handlers);
			LOGGER.info("Registered listener " + type + " --> " + listener);
		}
		handlers.add(listener);
	}

	public <T> void removeListener(Class<T> type, EventListener<T> listener) {
		if (type == null) {
			throw new IllegalArgumentException("type must not be null");
		}
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null");
		}
		List<EventListener> handlers = mapTypeToListener.get(type);
		if (handlers == null) {
			return;
		}
		handlers.remove(listener);
		if (handlers.isEmpty()) {
			mapTypeToListener.remove(type);
		}
		LOGGER.info("Deregistered listener " + type + " --> " + listener);
	}

	public <T> void removeAllListeners(Class<T> type) {
		if (type == null) {
			throw new IllegalArgumentException("type must not be null");
		}
		mapTypeToListener.remove(type);
		LOGGER.info("Deregistered all listeners for type " + type);
	}

	public <T> List<EventListener<T>> getListeners(Class<T> type) {
		if (type == null) {
			throw new IllegalArgumentException("type must not be null");
		}
		List<EventListener> listeners = mapTypeToListener.get(type);
		if (listeners == null) {
			return new ArrayList<EventListener<T>>();
		}
		// Java generics fun:
		ArrayList<EventListener<T>> returnValue = new ArrayList<EventListener<T>>();
		for (EventListener<T> l : listeners) {
			returnValue.add(l);
		}
		return returnValue;
	}

	public void emit(Object event) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		List<EventListener> listenerList = mapTypeToListener.get(event.getClass());
		if (listenerList != null) {
			eventDispatcher.dispatch(event, listenerList);
		}
	}

	public <T> void emit(T event, EventListener<T>... oneOffListeners) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		if (oneOffListeners == null || oneOffListeners.length == 0) {
			throw new IllegalArgumentException("listeners must not be null or empty");
		}
		eventDispatcher.dispatch(event, oneOffListeners);
	}

}
