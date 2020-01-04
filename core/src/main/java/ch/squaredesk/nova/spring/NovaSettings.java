package ch.squaredesk.nova.spring;

import ch.squaredesk.nova.events.EventDispatchConfig;

public class NovaSettings {
    public final String identifier;
    public final EventDispatchConfig eventDispatchConfig;
    public final Boolean captureJvmMetrics;

    public NovaSettings(String identifier, EventDispatchConfig eventDispatchConfig, Boolean captureJvmMetrics) {
        this.identifier = identifier;
        this.eventDispatchConfig = eventDispatchConfig;
        this.captureJvmMetrics = captureJvmMetrics;
    }
}
