package com.dotc.nova.process;

import com.dotc.nova.ProcessingLoop;
import com.dotc.nova.events.EventListener;

public class Process {
	private final ProcessingLoop eventDispatcher;

	public Process(ProcessingLoop eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
	}

	public void nextTick(final Runnable callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		eventDispatcher.dispatch(new EventListener() {
			@Override
			public void handle(Object... data) {
				callback.run();
			}
		});
	}
}
