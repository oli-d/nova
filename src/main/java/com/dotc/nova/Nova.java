package com.dotc.nova;

import com.dotc.nova.events.*;
import com.dotc.nova.filesystem.Filesystem;
import com.dotc.nova.timers.Timers;

public class Nova {
	private final ProcessingLoop processingLoop;

	public final Timers timers;
	public final EventEmitter eventEmitter;
	public final com.dotc.nova.process.Process process;
	public final Filesystem filesystem;

	private Nova(Builder builder) {
		processingLoop = new ProcessingLoop();
		processingLoop.init();

		timers = new Timers(processingLoop);
		if (builder.asyncEventEmitter) {
			eventEmitter = new AsyncEventEmitter(processingLoop);
		} else {
			eventEmitter = new SyncEventEmitter();
		}
		process = new com.dotc.nova.process.Process(processingLoop);
		filesystem = new Filesystem(process);
	}

	public static class Builder {
		private boolean asyncEventEmitter;

		public Builder withAsyncEventEmitter(boolean asyncEventEmitter) {
			this.asyncEventEmitter = asyncEventEmitter;
			return this;
		}

		public Nova build() {
			return new Nova(this);
		}
	}
}
