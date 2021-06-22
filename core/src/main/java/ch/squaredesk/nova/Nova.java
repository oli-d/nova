/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova;

import ch.squaredesk.nova.events.EventBus;
import ch.squaredesk.nova.events.EventDispatchConfig;
import ch.squaredesk.nova.events.EventDispatchMode;
import ch.squaredesk.nova.filesystem.Filesystem;
import io.reactivex.rxjava3.core.BackpressureStrategy;

public class Nova {

    public final String identifier;
    public final EventBus eventBus;
    public final Filesystem filesystem;

    private Nova(Builder builder) {
        this.identifier = builder.identifier;
        this.filesystem = new Filesystem();
        EventDispatchConfig dispatchConfig = new EventDispatchConfig(
                builder.defaultBackpressureStrategy,
                builder.warnOnUnhandledEvents,
                builder.eventDispatchMode,
                builder.parallelism
        );
        this.eventBus = new EventBus(identifier, dispatchConfig);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String identifier = "";
        private boolean warnOnUnhandledEvents = false;
        private BackpressureStrategy defaultBackpressureStrategy = BackpressureStrategy.BUFFER;
        private EventDispatchMode eventDispatchMode = EventDispatchMode.BLOCKING;
        private int parallelism = 0;

        private Builder() {
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setParallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        public Builder setEventDispatchMode(EventDispatchMode eventDispatchMode) {
            this.eventDispatchMode = eventDispatchMode;
            return this;
        }

        public Builder setDefaultBackpressureStrategy(BackpressureStrategy defaultBackpressureStrategy) {
            this.defaultBackpressureStrategy = defaultBackpressureStrategy;
            return this;
        }

        public Builder setWarnOnUnhandledEvents(boolean warnOnUnhandledEvents) {
            this.warnOnUnhandledEvents = warnOnUnhandledEvents;
            return this;
        }

        public Nova build() {
            return new Nova(this);
        }

    }
}
