/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.process;

import ch.squaredesk.nova.events.EventEmitter;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

public class Process implements Executor {
	private final EventEmitter eventEmitter;
	private final AtomicLong counter = new AtomicLong();

	public Process(EventEmitter eventEmitter) {
		this.eventEmitter = eventEmitter;
	}

	public void nextTick(Runnable callback) {
        requireNonNull(callback, "callback must not be null");
        String processNextTickDummyEvent = "ProcessNextTickDummyEvent" + counter.incrementAndGet();
        eventEmitter.single(processNextTickDummyEvent).subscribe(x -> callback.run());
		eventEmitter.emit(processNextTickDummyEvent);
	}

    @Override
    public void execute(Runnable command) {
        nextTick(command);
    }
}
