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

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.events.metrics.NoopEventMetricsCollector;

public class CurrentThreadEventEmitterTest extends EventEmitterTestBase {

    @Override
    public EventEmitter createEventEmitter() {
        EventDispatchConfig edc = new EventDispatchConfig.Builder().setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD).build();
        Nova nova = new Nova.Builder().setEventDispatchConfig(edc).build();

        return new CurrentThreadEventEmitter("testInstance", edc, nova.metrics);
    }

}
