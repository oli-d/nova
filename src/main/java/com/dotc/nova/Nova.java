package com.dotc.nova;

import com.dotc.nova.events.CurrentThreadEventEmitter;
import com.dotc.nova.events.EventDispatchConfig;
import com.dotc.nova.events.EventDispatchConfig.DispatchThreadStrategy;
import com.dotc.nova.events.EventEmitter;
import com.dotc.nova.events.EventLoop;
import com.dotc.nova.events.EventLoopAwareEventEmitter;
import com.dotc.nova.events.metrics.EventMetricsCollector;
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
		identifier = builder.identifier;

		EventMetricsCollector metricsCollector = new EventMetricsCollector(metrics, identifier);
		eventLoop = new EventLoop(builder.identifier, builder.eventDispatchConfig, metricsCollector);

		timers = new Timers(eventLoop);
		if (builder.eventDispatchConfig.dispatchThreadStrategy == DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD) {
			eventEmitter = new CurrentThreadEventEmitter(builder.eventDispatchConfig.warnOnUnhandledEvent, metricsCollector);
		} else {
			eventEmitter = new EventLoopAwareEventEmitter(eventLoop, builder.eventDispatchConfig.warnOnUnhandledEvent, metricsCollector);
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

		public Builder setIdentifier(String identifier) {
			this.identifier = identifier;
			return this;
		}

		public Builder setEventDispatchConfig(EventDispatchConfig eventDispatchConfig) {
			this.eventDispatchConfig = eventDispatchConfig;
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
