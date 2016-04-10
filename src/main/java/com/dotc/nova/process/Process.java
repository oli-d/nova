package com.dotc.nova.process;

import com.dotc.nova.events.EventLoop;

import java.util.concurrent.Executor;

public class Process implements Executor {
	private final EventLoop eventLoop;

	public Process(EventLoop eventLoop) {
		this.eventLoop = eventLoop;
	}

	public void nextTick(final Runnable callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		eventLoop.dispatch(data -> callback.run());
	}

    @Override
    public void execute(Runnable command) {
        nextTick(command);
    }
}
