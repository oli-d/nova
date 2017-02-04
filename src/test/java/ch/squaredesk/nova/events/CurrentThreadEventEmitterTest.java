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

import ch.squaredesk.nova.metrics.Metrics;

public class CurrentThreadEventEmitterTest extends EventEmitterTestBase {

    @Override
    public EventEmitter createEventEmitter() {
        return createEventEmitter(EventDispatchConfig.builder().build());
    }

    @Override
    protected EventEmitter createEventEmitter(EventDispatchConfig eventDispatchConfig) {
        return new CurrentThreadEventEmitter("testInstance", eventDispatchConfig.warnOnUnhandledEvent, new Metrics());
    }

}
