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
import ch.squaredesk.nova.events.EventDispatchConfig;
import ch.squaredesk.nova.filesystem.Filesystem;
import ch.squaredesk.nova.metrics.CpuMeter;
import ch.squaredesk.nova.metrics.GarbageCollectionMeter;
import ch.squaredesk.nova.metrics.MemoryMeter;
import ch.squaredesk.nova.metrics.Metrics;

public class Nova {

    public final String identifier;
    public final EventBus eventBus;
    public final Filesystem filesystem;
    public final Metrics metrics;

    private Nova(Builder builder) {
        this.identifier = builder.identifier;
        this.eventBus = new EventBus(identifier, builder.eventDispatchConfig, builder.metrics);
        this.filesystem = new Filesystem();
        this.metrics = builder.metrics;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String identifier = "";
        private Metrics metrics;
        private boolean captureJvmMetrics = true;
        private EventDispatchConfig eventDispatchConfig;

        private Builder() {
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setEventDispatchConfig(EventDispatchConfig eventDispatchConfig) {
            this.eventDispatchConfig = eventDispatchConfig;
            return this;
        }

        public Builder captureJvmMetrics(boolean captureJvmMetrics) {
            this.captureJvmMetrics = captureJvmMetrics;
            return this;
        }


        public Nova build() {
            if (metrics == null) {
                metrics = new Metrics();
            }

            if (captureJvmMetrics) {
                metrics.register(new MemoryMeter(),"jvm", "mem");
                metrics.register(new GarbageCollectionMeter(),"jvm", "gc");
                CpuMeter cpuMeter = new CpuMeter();
                if (cpuMeter.environmentSupportsCpuMetrics()) {
                    metrics.register(cpuMeter, "os", "cpu");
                }
            }

            return new Nova(this);
        }

    }
}
