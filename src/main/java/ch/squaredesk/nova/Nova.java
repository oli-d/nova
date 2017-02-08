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

import ch.squaredesk.nova.events.EventLoopConfig;
import ch.squaredesk.nova.events.EventLoop;
import ch.squaredesk.nova.filesystem.Filesystem;
import ch.squaredesk.nova.events.Process;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.events.Timers;

public class Nova {

	public final String identifier;
	public final Timers timers;
	public final EventLoop eventLoop;
	public final Process process;
	public final Filesystem filesystem;
	public final Metrics metrics;

	private Nova(Builder builder) {
		metrics = builder.metrics;
		identifier = builder.identifier;

		eventLoop = new EventLoop(identifier, builder.eventLoopConfig, metrics);
		timers = new Timers(eventLoop);
		process = new Process(eventLoop);
		filesystem = new Filesystem();
	}

	public static Builder builder() {
	    return new Builder();
    }

	public static class Builder {
		private String identifier;
		private EventLoopConfig eventLoopConfig;
		private Metrics metrics;

		private Builder() {
        }

		public Builder setIdentifier(String identifier) {
			this.identifier = identifier;
			return this;
		}

		public Builder setEventLoopConfig(EventLoopConfig eventLoopConfig) {
			this.eventLoopConfig = eventLoopConfig;
			return this;
		}

		public Builder setMetrics(Metrics metrics) {
			this.metrics = metrics;
			return this;
		}

		public Nova build() {
			if (eventLoopConfig == null) {
				eventLoopConfig = EventLoopConfig.builder().build();
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
