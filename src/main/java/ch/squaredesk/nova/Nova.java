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
import ch.squaredesk.nova.filesystem.Filesystem;
import ch.squaredesk.nova.process.Process;
import ch.squaredesk.nova.events.CurrentThreadEventEmitter;
import ch.squaredesk.nova.events.EventLoopAwareEventEmitter;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.timers.Timers;

public class Nova {

	public final String identifier;

	public final Timers timers;
	public final EventEmitter eventEmitter;
	public final Process process;
	public final Filesystem filesystem;
	public final Metrics metrics;

	private Nova(Builder builder) {
		metrics = builder.metrics;
		identifier = builder.identifier;

		eventEmitter = createEventEmitter(
				identifier,
				builder.eventDispatchConfig,
				metrics);
		timers = new Timers(eventEmitter);
		process = new Process(eventEmitter);
		filesystem = new Filesystem(process);
	}

	private EventEmitter createEventEmitter(
			String identifier,
			EventDispatchConfig eventDispatchConfig,
			Metrics metrics) {
		EventEmitter retVal;
		if (eventDispatchConfig.dispatchThreadStrategy == EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD) {
			retVal = new CurrentThreadEventEmitter(identifier, eventDispatchConfig, metrics);
		} else {
			retVal = new EventLoopAwareEventEmitter(identifier, eventDispatchConfig, metrics);
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
