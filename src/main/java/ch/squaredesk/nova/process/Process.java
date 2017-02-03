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

import ch.squaredesk.nova.events.EventLoop;
import io.reactivex.Emitter;

import java.util.concurrent.Executor;

public class Process implements Executor {
	private final EventLoop eventLoop;

	public Process(EventLoop eventLoop) {
		this.eventLoop = eventLoop;
	}

	public void nextTick(Runnable callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
        Emitter<Object[]> wrapper = new Emitter<Object[]>() {
            @Override
            public void onNext(Object[] value) {
                callback.run();;
            }
            @Override
            public void onError(Throwable error) {
            }
            @Override
            public void onComplete() {
            }
        };
		eventLoop.dispatch(wrapper);
	}

    @Override
    public void execute(Runnable command) {
        nextTick(command);
    }
}
