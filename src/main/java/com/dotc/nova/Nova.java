package com.dotc.nova;

import com.dotc.nova.events.*;
import com.dotc.nova.events.EventDispatchConfig.DispatchThreadStrategy;
import com.dotc.nova.filesystem.Filesystem;
import com.dotc.nova.timers.Timers;

public class Nova {
	private final EventLoop eventLoop;

	public final Timers timers;
	public final EventEmitter eventEmitter;
	public final com.dotc.nova.process.Process process;
	public final Filesystem filesystem;

	private Nova(Builder builder) {
		eventLoop = new EventLoop(builder.eventDispatchConfig);

		timers = new Timers(eventLoop);
		if (builder.eventDispatchConfig.dispatchThreadStrategy == DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD) {
			eventEmitter = new CurrentThreadEventEmitter(builder.eventDispatchConfig.warnOnUnhandledEvent);
		} else {
			eventEmitter = new EventLoopAwareEventEmitter(eventLoop, builder.eventDispatchConfig.warnOnUnhandledEvent);
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
		private EventDispatchConfig eventDispatchConfig;

		public Builder setEventDispatchConfig(EventDispatchConfig eventDispatchConfig) {
			this.eventDispatchConfig = eventDispatchConfig;
			return this;
		}

		public Nova build() {
			if (eventDispatchConfig == null) {
				eventDispatchConfig = new EventDispatchConfig.Builder().build();
			}
			return new Nova(this);
		}
	}
}
