package com.dotc.nova.events;

import java.util.List;

import com.dotc.nova.events.metrics.EventMetricsCollector;

public class EventLoopAwareEventEmitter extends EventEmitter {
	private final EventLoop eventLoop;

	public EventLoopAwareEventEmitter(EventLoop eventLoop, boolean warnOnUnhandledEvents,
			EventMetricsCollector metricsCollector) {
		super(warnOnUnhandledEvents, metricsCollector);
		this.eventLoop = eventLoop;
	}

	@Override
	<EventType> void dispatchEventAndDataToListeners(List<EventListener> listenerList, EventType event, Object... data) {
		eventLoop.dispatch(event, listenerList, data);
	}

}
