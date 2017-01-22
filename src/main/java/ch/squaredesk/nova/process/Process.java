/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.process;

import ch.squaredesk.nova.events.EventLoop;

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
