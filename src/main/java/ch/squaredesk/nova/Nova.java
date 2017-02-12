/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova;

import ch.squaredesk.nova.events.EventBus;
import ch.squaredesk.nova.events.EventBusConfig;
import ch.squaredesk.nova.filesystem.Filesystem;
import ch.squaredesk.nova.metrics.Metrics;

public class Nova {

    public final String identifier;
    public final EventBus eventBus;
    public final Filesystem filesystem;
    public final Metrics metrics;

    private Nova(Builder builder) {
        metrics = builder.metrics;
        identifier = builder.identifier;
        eventBus = new EventBus(identifier, builder.eventBusConfig, metrics);
        filesystem = new Filesystem();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String identifier;
        private EventBusConfig eventBusConfig;
        private Metrics metrics;

        private Builder() {
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setEventBusConfig(EventBusConfig eventBusConfig) {
            this.eventBusConfig = eventBusConfig;
            return this;
        }

        public Builder setMetrics(Metrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public Nova build() {
            if (eventBusConfig == null) {
                eventBusConfig = EventBusConfig.builder().build();
            }
            if (identifier == null) {
                identifier = "Nova";
            }
            if (metrics == null) {
                metrics = new Metrics();
            }

            return new Nova(this);
        }
    }
}
