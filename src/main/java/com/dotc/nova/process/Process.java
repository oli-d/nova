package com.dotc.nova.process;

import com.dotc.nova.ProcessingLoop;

public class Process {
	private final ProcessingLoop eventDispatcher;

	public Process(ProcessingLoop eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
	}

	public void nextTick(Runnable callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		eventDispatcher.dispatch(callback);
	}
}
