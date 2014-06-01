package com.dotc.nova;

import com.dotc.nova.events.*;
import com.dotc.nova.events.EventDispatchConfig.DispatchThreadStrategy;
import com.dotc.nova.events.metrics.*;
import com.dotc.nova.filesystem.Filesystem;
import com.dotc.nova.metrics.Metrics;
import com.dotc.nova.timers.Timers;

public class Nova {

	public final String identifier;

	public final EventLoop eventLoop;
	public final Timers timers;
	public final EventEmitter eventEmitter;
	public final com.dotc.nova.process.Process process;
	public final Filesystem filesystem;
	public final Metrics metrics;

	private Nova(Builder builder) {
		metrics = new Metrics();
		EventMetricsCollector metricsCollector;
		if (builder.metricsEnabled) {
			metricsCollector = new DefaultEventMetricsCollector(metrics);
		} else {
			metricsCollector = new NoopEventMetricsCollector();
		}

		eventLoop = new EventLoop(builder.identifier, builder.eventDispatchConfig, metricsCollector);
		identifier = builder.identifier;

		timers = new Timers(eventLoop);
		if (builder.eventDispatchConfig.dispatchThreadStrategy == DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD) {
			eventEmitter = new CurrentThreadEventEmitter(builder.eventDispatchConfig.warnOnUnhandledEvent,
					metricsCollector);
		} else {
			eventEmitter = new EventLoopAwareEventEmitter(eventLoop, builder.eventDispatchConfig.warnOnUnhandledEvent,
					metricsCollector);
		}
		process = new com.dotc.nova.process.Process(eventLoop);
		filesystem = new Filesystem(process);
	}

	public Timers getTimers() {
		return timers;
	}

	public EventEmitter getEventEmitter() {
		return eventEmitter;
	}

	public static class Builder {
		private String identifier;
		private EventDispatchConfig eventDispatchConfig;
		private boolean metricsEnabled = true;

		public Builder setIdentifier(String identifier) {
			this.identifier = identifier;
			return this;
		}

		public Builder setEventDispatchConfig(EventDispatchConfig eventDispatchConfig) {
			this.eventDispatchConfig = eventDispatchConfig;
			return this;
		}

		public Builder setMetricsEnabled(boolean metricsEnabled) {
			this.metricsEnabled = metricsEnabled;
			return this;
		}

		public Nova build() {
			if (eventDispatchConfig == null) {
				eventDispatchConfig = new EventDispatchConfig.Builder().build();
			}
			if (identifier == null) {
				identifier = "";
			}

			return new Nova(this);
		}
	}
}
