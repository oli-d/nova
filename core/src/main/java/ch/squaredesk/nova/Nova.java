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
import ch.squaredesk.nova.metrics.CpuMeter;
import ch.squaredesk.nova.metrics.GarbageCollectionMeter;
import ch.squaredesk.nova.metrics.MemoryMeter;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;

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
        private String identifier = "";
        private Metrics metrics;
        private boolean captureJvmMetrics = true;

        private BackpressureStrategy defaultBackpressureStrategy = BackpressureStrategy.BUFFER;
        private boolean warnOnUnhandledEvent = false;
        private EventBusConfig eventBusConfig;

        private Builder() {
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setDefaultBackpressureStrategy(BackpressureStrategy defaultBackpressureStrategy) {
            this.defaultBackpressureStrategy = defaultBackpressureStrategy;
            return this;
        }

        public Builder setWarnOnUnhandledEvent(boolean warnOnUnhandledEvent) {
            this.warnOnUnhandledEvent = warnOnUnhandledEvent;
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

            eventBusConfig = new EventBusConfig(defaultBackpressureStrategy, warnOnUnhandledEvent);

            return new Nova(this);
        }
    }
}
