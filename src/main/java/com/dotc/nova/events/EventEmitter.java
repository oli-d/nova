package com.dotc.nova.events;

import java.util.*;

import org.apache.log4j.Logger;

import com.dotc.nova.ProcessingLoop;

public class EventEmitter {
	private static final Logger LOGGER = Logger.getLogger(EventEmitter.class);

	private final HashMap<Object, List<EventHandler>> mapEventToHandler = new HashMap<Object, List<EventHandler>>();

	private final ProcessingLoop eventDispatcher;

	public EventEmitter(ProcessingLoop eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
	}

	public void on(Object event, EventHandler callback) {
		addListener(event, callback);
	}

	public void addListener(Object event, EventHandler callback) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		if (callback == null) {
			throw new IllegalArgumentException("handler must not be null");
		}
		List<EventHandler> handlers = mapEventToHandler.get(event);
		if (handlers == null) {
			handlers = new ArrayList<>();
			mapEventToHandler.put(event, handlers);
			LOGGER.debug("Registered event " + event + " --> " + callback);
		}
		handlers.add(callback);
	}

	public void removeListener(Object event, EventHandler handler) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		if (handler == null) {
			throw new IllegalArgumentException("handler must not be null");
		}
		List<EventHandler> handlers = mapEventToHandler.get(event);
		if (handlers == null) {
			return;
		}
		handlers.remove(handler);
		if (handlers.isEmpty()) {
			mapEventToHandler.remove(event);
		}
		LOGGER.debug("Deregistered handler " + event + " --> " + handler);
	}

	public void removeAllListeners(Object event) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		mapEventToHandler.remove(event);
		LOGGER.debug("Deregistered all listeners for event " + event);
	}

	public List<EventHandler> getHandlers(Object event) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		List<EventHandler> listeners = mapEventToHandler.get(event);
		if (listeners == null) {
			return new ArrayList<>();
		} else {
			return listeners;
		}
	}

	public void emit(Object event) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		List<EventHandler> listenerList = mapEventToHandler.get(event);
		if (listenerList != null) {
			eventDispatcher.dispatch(event, listenerList);
		}
	}

	public void emit(Object event, Object... data) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be null");
		}
		List<EventHandler> listenerList = mapEventToHandler.get(event);
		if (listenerList != null) {
			eventDispatcher.dispatch(event, listenerList, data);
		}
	}

}
