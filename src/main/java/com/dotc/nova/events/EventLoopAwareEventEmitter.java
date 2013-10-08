package com.dotc.nova.events;

import java.util.List;


public class EventLoopAwareEventEmitter extends EventEmitter {
	private final EventLoop eventLoop;

	public EventLoopAwareEventEmitter(EventLoop eventLoop) {
		this.eventLoop = eventLoop;
	}

	@Override
	<EventType, ParameterType> void dispatchEventAndDataToListeners(List<EventListener> listenerList, EventType event, ParameterType... data) {
		eventLoop.dispatch(event, listenerList, data);
	}

}
