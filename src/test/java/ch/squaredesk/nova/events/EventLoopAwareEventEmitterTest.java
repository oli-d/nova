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

import ch.squaredesk.nova.events.metrics.NoopEventMetricsCollector;

public class EventLoopAwareEventEmitterTest extends EventEmitterTestBase {

	@Override
	public EventEmitter createEventEmitter() {
//        EventDispatchConfig edc = new EventDispatchConfig.Builder()
//                .setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD)
//                .build();
//        EventLoop eventLoop = new EventLoop("id",edc,new NoopEventMetricsCollector());
//		return new EventLoopAwareEventEmitter(eventLoop, new NoopEventMetricsCollector(), false);
		return null;
	}

}
