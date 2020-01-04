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

package ch.squaredesk.nova.autoconfig;

import io.reactivex.BackpressureStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nova")
public class NovaSettings {
    /** The identifier of the Nova instance */
    private String identifier;
    /** The default backpressure strategy to use for the event Flowables */
    private BackpressureStrategy defaultBackpressureStrategy = BackpressureStrategy.BUFFER;
    /** Specifies if the system should log a warning when an event is emitted, for which no handler was registered */
    private Boolean warnOnUnhandledEvent = false;
    /** Specifies if the system should capture metrics */
    private Boolean captureJvmMetrics = true;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public BackpressureStrategy getDefaultBackpressureStrategy() {
        return defaultBackpressureStrategy;
    }

    public void setDefaultBackpressureStrategy(BackpressureStrategy defaultBackpressureStrategy) {
        this.defaultBackpressureStrategy = defaultBackpressureStrategy;
    }

    public Boolean getWarnOnUnhandledEvent() {
        return warnOnUnhandledEvent;
    }

    public void setWarnOnUnhandledEvent(Boolean warnOnUnhandledEvent) {
        this.warnOnUnhandledEvent = warnOnUnhandledEvent;
    }

    public Boolean getCaptureJvmMetrics() {
        return captureJvmMetrics;
    }

    public void setCaptureJvmMetrics(Boolean captureJvmMetrics) {
        this.captureJvmMetrics = captureJvmMetrics;
    }
}
