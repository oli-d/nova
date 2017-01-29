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

import ch.squaredesk.nova.events.EventDispatchConfig;
import ch.squaredesk.nova.events.EventEmitter;
import ch.squaredesk.nova.events.EventLoop;
import ch.squaredesk.nova.events.metrics.EventMetricsCollector;
import ch.squaredesk.nova.filesystem.Filesystem;
import ch.squaredesk.nova.process.Process;
import ch.squaredesk.nova.events.CurrentThreadEventEmitter;
import ch.squaredesk.nova.events.EventLoopAwareEventEmitter;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.timers.Timers;

public class Nova {

	public final String identifier;

	public final EventLoop eventLoop;
	public final Timers timers;
	public final EventEmitter eventEmitter;
	public final Process process;
	public final Filesystem filesystem;
	public final Metrics metrics;

	private Nova(Builder builder) {
		metrics = builder.metrics;
		identifier = builder.identifier;

		EventMetricsCollector eventMetricsCollector = new EventMetricsCollector(builder.metrics, builder.identifier);
		eventLoop = createEventLoop(
				identifier,
				builder.eventDispatchConfig,
				eventMetricsCollector);
		metrics.register(eventLoop.getMetrics(), identifier);
		eventEmitter = createEventEmitter(
				builder.eventDispatchConfig,
				eventMetricsCollector);
		timers = new Timers(eventLoop);
		process = new Process(eventLoop);
		filesystem = new Filesystem(process);
	}

	private EventLoop createEventLoop(
			String identifier,
			EventDispatchConfig eventDispatchConfig,
			EventMetricsCollector eventMetricsCollector) {
		return new EventLoop(identifier, eventDispatchConfig, eventMetricsCollector);
	}

	private EventEmitter createEventEmitter(
			EventDispatchConfig eventDispatchConfig,
			EventMetricsCollector eventMetricsCollector) {
		EventEmitter retVal;
		if (eventDispatchConfig.dispatchThreadStrategy == EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD) {
			retVal = new CurrentThreadEventEmitter(
					eventMetricsCollector,eventDispatchConfig.warnOnUnhandledEvent);
		} else {
			retVal = new EventLoopAwareEventEmitter(
					eventLoop, eventMetricsCollector,eventDispatchConfig.warnOnUnhandledEvent);
		}
		return retVal;
	}

	public static class Builder {
		private String identifier;
		private EventDispatchConfig eventDispatchConfig;
		private Metrics metrics;

		public Builder setIdentifier(String identifier) {
			this.identifier = identifier;
			return this;
		}

		public Builder setEventDispatchConfig(EventDispatchConfig eventDispatchConfig) {
			this.eventDispatchConfig = eventDispatchConfig;
			return this;
		}

		public Builder setMetrics(Metrics metrics) {
			this.metrics = metrics;
			return this;
		}

		public Nova build() {
			if (eventDispatchConfig == null) {
				eventDispatchConfig = new EventDispatchConfig.Builder().build();
			}
			if (identifier == null) {
				identifier = "";
			}
			if (metrics == null) {
				metrics = new Metrics();
			}

			return new Nova(this);
		}
	}
}
