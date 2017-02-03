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

import ch.squaredesk.nova.events.metrics.EventMetricsCollector;
import io.reactivex.Emitter;

import java.util.List;

public class EventLoopAwareEventEmitter extends EventEmitter {
	private final EventLoop eventLoop;

	public EventLoopAwareEventEmitter(EventLoop eventLoop, EventMetricsCollector eventMetricsCollector, boolean warnOnUnhandledEvents) {
		super(eventMetricsCollector, warnOnUnhandledEvents);
		this.eventLoop = eventLoop;
	}

	@Override
	void dispatchEventAndData(List<Emitter<Object[]>> emitterList, Object event, Object... data) {
		eventLoop.dispatch(event, emitterList, data);
	}

}
