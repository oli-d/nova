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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

public class Process  {
    static final String DUMMY_NEXT_TICK_EVENT_PREFIX = "ProcessNextTickDummyEvent";
    private final Logger logger = LoggerFactory.getLogger(Process.class);

	private final EventLoop eventLoop;
	private final AtomicLong counter = new AtomicLong();

	public Process(EventLoop eventLoop) {
		this.eventLoop = eventLoop;
	}

	public void nextTick(Runnable callback) {
        requireNonNull(callback, "callback must not be null");
        String newId = String.valueOf(counter.incrementAndGet());
        String timeoutDummyEvent = DUMMY_NEXT_TICK_EVENT_PREFIX + newId;
        eventLoop
                .single(timeoutDummyEvent)
                .subscribe(
                        x -> {
                            try {
                                callback.run();
                            } catch (Throwable t) {
                                logger.error("An error occurred trying to invoke nextTick() ",t);
                            }
                        },
                        throwable -> {
                            logger.error("An error was pushed to nextTick() invoker", throwable);
                        }
                );
        eventLoop.emit(timeoutDummyEvent);
	}
}
