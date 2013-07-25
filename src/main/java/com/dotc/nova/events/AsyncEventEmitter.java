package com.dotc.nova.events;

import java.util.List;

import com.dotc.nova.ProcessingLoop;

public class AsyncEventEmitter extends EventEmitter {
	private final ProcessingLoop eventDispatcher;

	public AsyncEventEmitter(ProcessingLoop eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
	}

	@Override
	<EventType, ParameterType> void dispatchEventAndDataToListeners(List<EventListener> listenerList, EventType event, ParameterType... data) {
		eventDispatcher.dispatch(event, listenerList, data);
	}

}
