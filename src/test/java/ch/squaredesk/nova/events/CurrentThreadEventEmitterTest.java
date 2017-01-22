/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events;

import ch.squaredesk.nova.events.metrics.NoopEventMetricsCollector;

public class CurrentThreadEventEmitterTest extends EventEmitterTestBase {

    @Override
    public EventEmitter createEventEmitter() {
        return new CurrentThreadEventEmitter(new NoopEventMetricsCollector(), false);
    }

}
