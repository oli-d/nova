/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events.annotation;

import ch.squaredesk.nova.events.EventBus;
import ch.squaredesk.nova.metrics.Metrics;

public class EventContext {
    public final Metrics metrics;
    public final EventBus eventBus;

    public EventContext(Metrics metrics, EventBus eventBus) {
        this.metrics = metrics;
        this.eventBus = eventBus;
    }
}
