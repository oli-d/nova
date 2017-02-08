/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events;

public class EventLoopConfig {
	public static enum InsufficientCapacityStrategy {
		DROP_EVENTS, QUEUE_EVENTS, THROW_EXCEPTION, WAIT_UNTIL_SPACE_AVAILABLE
	};

	public static enum DispatchThreadStrategy {
		DISPATCH_IN_EMITTER_THREAD, DISPATCH_IN_SPECIFIC_THREAD
	};

	public final InsufficientCapacityStrategy insufficientCapacityStrategy;
	public final DispatchThreadStrategy dispatchThreadStrategy;
	public final boolean warnOnUnhandledEvent;

	public EventLoopConfig(Builder builder) {
		builder.validate();
		this.insufficientCapacityStrategy = builder.insufficientCapacityStrategy;
		this.dispatchThreadStrategy = builder.dispatchThreadStrategy;
		this.warnOnUnhandledEvent = builder.warnOnUnhandledEvent;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private InsufficientCapacityStrategy insufficientCapacityStrategy = InsufficientCapacityStrategy.THROW_EXCEPTION;
		private DispatchThreadStrategy dispatchThreadStrategy = DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD;
		private boolean warnOnUnhandledEvent;

		private Builder() {
		}

		public Builder setInsufficientCapacityStrategy(InsufficientCapacityStrategy queueFullStrategy) {
			this.insufficientCapacityStrategy = queueFullStrategy;
			return this;
		}

		public Builder setDispatchThreadStrategy(DispatchThreadStrategy dispatchThreadStrategy) {
			this.dispatchThreadStrategy = dispatchThreadStrategy;
			return this;
		}

		public Builder setWarnOnUnhandledEvent(boolean warnOnUnhandledEvent) {
			this.warnOnUnhandledEvent = warnOnUnhandledEvent;
			return this;
		}

		private void validate() {
		}

		public EventLoopConfig build() {
			validate();
			return new EventLoopConfig(this);
		}
	}
}
