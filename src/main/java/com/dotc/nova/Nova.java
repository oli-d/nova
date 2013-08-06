package com.dotc.nova;

import com.dotc.nova.events.*;
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
		if (builder.currentThreadEventEmitter) {
			eventEmitter = new CurrentThreadEventEmitter();
		} else {
			eventEmitter = new EventLoopAwareEventEmitter(eventLoop);
		}
		process = new com.dotc.nova.process.Process(eventLoop);
		filesystem = new Filesystem(process);
	}

	public static class Builder {
		private boolean currentThreadEventEmitter;
		private EventDispatchConfig eventDispatchConfig;

		public Builder withEventDispatchConfig(EventDispatchConfig eventDispatchConfig) {
			this.eventDispatchConfig = eventDispatchConfig;
			return this;
		}

		public Builder withCurrentThreadEventEmitter(boolean currentThreadEventEmitter) {
			this.currentThreadEventEmitter = currentThreadEventEmitter;
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
