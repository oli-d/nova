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

import io.reactivex.BackpressureStrategy;

public class EventDispatchConfig {
    public final BackpressureStrategy defaultBackpressureStrategy;
    public final boolean warnOnUnhandledEvents;
    public final boolean dispatchEventsOnSeparateExecutor;
    public final int eventDispatchThreadPoolSize;


    public EventDispatchConfig(BackpressureStrategy backpressureStrategy, boolean warnOnUnhandledEvents, boolean dispatchEventsOnSeparateExecutor, int eventDispatchThreadPoolSize) {
        this.defaultBackpressureStrategy = backpressureStrategy;
        this.warnOnUnhandledEvents = warnOnUnhandledEvents;
        this.dispatchEventsOnSeparateExecutor = dispatchEventsOnSeparateExecutor;
        this.eventDispatchThreadPoolSize = eventDispatchThreadPoolSize;
    }

    @Override
    public String toString() {
        return "EventBusConfig{" +
                "defaultBackpressureStrategy=" + defaultBackpressureStrategy +
                "dispatchEventsOnSeparateExecutor=" + dispatchEventsOnSeparateExecutor +
                "eventDispatchThreadPoolSize=" + eventDispatchThreadPoolSize +
                ", warnOnUnhandledEvents=" + warnOnUnhandledEvents +
                '}';
    }
}
