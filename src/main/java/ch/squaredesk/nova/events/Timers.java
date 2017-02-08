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

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

public class Timers {
	private static final Logger logger = LoggerFactory.getLogger(Timers.class);
    static final String DUMMY_TIMEOUT_EVENT_PREFIX = "TimersTimeoutDummyEvent";
    static final String DUMMY_INTERVAL_EVENT_PREFIX = "TimersIntervalDummyEvent";

    private final AtomicLong counter = new AtomicLong();

	private final EventLoop eventLoop;

	public Timers(EventLoop eventLoop) {
		this.eventLoop = eventLoop;
	}

    /**
     * To schedule execution of a one-time callback after delay milliseconds. Returns a timeoutId for possible use with clearTimeout().
     *
     * It is important to note that your callback will probably not be called in exactly <delay> ms - Nova makes no guarantees about the
     * exact timing of when the callback will fire, nor of the ordering things will fire in. The callback will be called as close as
     * possible to the time specified.
     */
	public Disposable setTimeout(Runnable callback, long delay) {
	    return setTimeout(callback, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * To schedule execution of a one-time callback after a specified delay . Returns a timeoutId for possible use with clearTimeout().
     *
     * It is important to note that your callback will probably not be called in exactly <delay> - Nova makes no guarantees about the
     * exact timing of when the callback will fire, nor of the ordering things will fire in. The callback will be called as close as
     * possible to the time specified.
     */
	public Disposable setTimeout(Runnable callback, long delay, TimeUnit timeUnit) {
		requireNonNull(callback, "callback must not be null");
		requireNonNull(timeUnit, "timeUnit must not be null");
        String newId = String.valueOf(counter.incrementAndGet());
        String timeoutDummyEvent = DUMMY_TIMEOUT_EVENT_PREFIX + newId;
        Disposable single = eventLoop
                .single(timeoutDummyEvent)
                .delay(delay, timeUnit)
                .subscribe(
                		x -> {
							try {
								callback.run();
							} catch (Throwable t) {
								logger.error("An error occurred trying to invoke timeout with ID " + newId,t);
							}
						},
						throwable -> {
							logger.error("An error was pushed to timeout with ID " + newId, throwable);
						}
				);
		eventLoop.emit(timeoutDummyEvent);
		return single;
	}

	/**
	 * To schedule the repeated execution of callback every interval milliseconds. Returns a intervalId for possible use with clearInterval().
	 *
	 */
	public Disposable setInterval(Runnable callback, long interval) {
		return setInterval(callback, 0, interval, TimeUnit.MILLISECONDS);
	}

	/**
	 * To schedule the repeated execution of callback. Returns a intervalId for possible use with clearInterval().
	 *
	 */
	public Disposable setInterval(Runnable callback, long initialDelay, long interval, TimeUnit timeUnit) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(timeUnit, "timeUnit must not be null");
        String newId = String.valueOf(counter.incrementAndGet());
        String intervalDummyEvent = DUMMY_INTERVAL_EVENT_PREFIX + newId;
        Disposable callbackInvoker = eventLoop
                .observe(intervalDummyEvent)
                .subscribe(
                		x -> {
                			try {
								callback.run();
							} catch (Throwable t) {
                                //	logger.error("An error occurred trying to invoke timeout with ID " + newId,t);
								// if we do not propagate the error, the interval will not stop
								Exceptions.propagate(t);
							}
						});
        // we create another "wrapper Observable" to trigger the callback invocation over the EventLoop
        // thus we make sure that the event is always propagated via the EventLoop dispatch thread
        Disposable recurringEventEmitter = Observable
                .interval(initialDelay, interval, timeUnit)
                .takeWhile(counter -> !callbackInvoker.isDisposed()) // important! stops the interval when callBackInvoker dies because of error
                .doFinally(() -> callbackInvoker.dispose())
                .subscribe(
                        counter -> eventLoop.emit(intervalDummyEvent),
                        error -> logger.error("An error was pushed to interval dummy event emitter for intervalId " + newId, error),
                        () -> callbackInvoker.dispose());

        return recurringEventEmitter;
	}

}
