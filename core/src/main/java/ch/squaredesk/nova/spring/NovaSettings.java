package ch.squaredesk.nova.spring;

import io.reactivex.BackpressureStrategy;

public class NovaSettings {
    public final String identifier;
    public final BackpressureStrategy defaultBackpressureStrategy;
    public final Boolean warnOnUnhandledEvent;
    public final Boolean captureJvmMetrics;

    public NovaSettings(String identifier, BackpressureStrategy defaultBackpressureStrategy, Boolean warnOnUnhandledEvent, Boolean captureJvmMetrics) {
        this.identifier = identifier;
        this.defaultBackpressureStrategy = defaultBackpressureStrategy;
        this.warnOnUnhandledEvent = warnOnUnhandledEvent;
        this.captureJvmMetrics = captureJvmMetrics;
    }
}
