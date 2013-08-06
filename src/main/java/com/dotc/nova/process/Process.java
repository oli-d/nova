package com.dotc.nova.process;

import com.dotc.nova.EventLoop;
import com.dotc.nova.events.EventListener;

public class Process {
	private final EventLoop eventLoop;

	public Process(EventLoop eventLoop) {
		this.eventLoop = eventLoop;
	}

	public void nextTick(final Runnable callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		eventLoop.dispatch(new EventListener() {
			@Override
			public void handle(Object... data) {
				callback.run();
			}
		});
	}
}
