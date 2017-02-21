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

public class EventBusConfig {
    public final BackpressureStrategy defaultBackpressureStrategy;
    public final boolean warnOnUnhandledEvent;

    public EventBusConfig(Builder builder) {
        this.defaultBackpressureStrategy = builder.defaultBackpressureStrategy;
        this.warnOnUnhandledEvent = builder.warnOnUnhandledEvent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BackpressureStrategy defaultBackpressureStrategy = BackpressureStrategy.BUFFER;
        private boolean warnOnUnhandledEvent;

        private Builder() {
        }

        public Builder setDefaultBackpressureStrategy(BackpressureStrategy defaultBackpressureStrategy) {
            this.defaultBackpressureStrategy = defaultBackpressureStrategy;
            return this;
        }

        public Builder setWarnOnUnhandledEvent(boolean warnOnUnhandledEvent) {
            this.warnOnUnhandledEvent = warnOnUnhandledEvent;
            return this;
        }

        private void validate() {
        }

        public EventBusConfig build() {
            validate();
            return new EventBusConfig(this);
        }
    }
}
