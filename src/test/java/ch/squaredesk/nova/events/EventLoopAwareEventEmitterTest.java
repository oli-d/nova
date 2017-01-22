/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events;

import ch.squaredesk.nova.events.metrics.NoopEventMetricsCollector;

public class EventLoopAwareEventEmitterTest extends EventEmitterTestBase {

	@Override
	public EventEmitter createEventEmitter() {
        EventDispatchConfig edc = new EventDispatchConfig.Builder()
                .setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD)
                .build();
        EventLoop eventLoop = new EventLoop("id",edc,new NoopEventMetricsCollector());
		return new EventLoopAwareEventEmitter(eventLoop, new NoopEventMetricsCollector(), false);
	}

}
