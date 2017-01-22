/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events;

import ch.squaredesk.nova.events.metrics.EventMetricsCollector;

import java.util.List;

public class EventLoopAwareEventEmitter extends EventEmitter {
	private final EventLoop eventLoop;

	public EventLoopAwareEventEmitter(EventLoop eventLoop, EventMetricsCollector eventMetricsCollector, boolean warnOnUnhandledEvents) {
		super(eventMetricsCollector, warnOnUnhandledEvents);
		this.eventLoop = eventLoop;
	}

	@Override
	void dispatchEventAndDataToListeners(List<EventListener> listenerList, Object event, Object... data) {
		eventLoop.dispatch(event, listenerList, data);
	}

}
