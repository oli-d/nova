/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.events;

import io.reactivex.BackpressureStrategy;

import java.util.Objects;

public class EventDispatchConfig {
    public final BackpressureStrategy defaultBackpressureStrategy;
    public final boolean warnOnUnhandledEvents;
    public final EventDispatchMode eventDispatchMode;
    public final int parallelism;


    public EventDispatchConfig(BackpressureStrategy defaultBackpressureStrategy,
                               boolean warnOnUnhandledEvents,
                               EventDispatchMode eventDispatchMode,
                               int parallelism) {
        this.defaultBackpressureStrategy = Objects.requireNonNull(defaultBackpressureStrategy);
        this.warnOnUnhandledEvents = warnOnUnhandledEvents;
        this.eventDispatchMode = Objects.requireNonNull(eventDispatchMode);
        this.parallelism = parallelism;
    }

    @Override
    public String toString() {
        return "EventDispatchonfig{" +
                "defaultBackpressureStrategy=" + defaultBackpressureStrategy +
                ", eventDispatchMode=" + eventDispatchMode +
                ", parallelism=" + parallelism +
                ", warnOnUnhandledEvents=" + warnOnUnhandledEvents +
                '}';
    }
}
