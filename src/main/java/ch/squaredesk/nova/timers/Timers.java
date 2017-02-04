/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.timers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import ch.squaredesk.nova.events.EventEmitter;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

public class Timers {
	private static final Logger logger = LoggerFactory.getLogger(Timers.class);

	private final AtomicLong counter = new AtomicLong();

	private final EventEmitter eventEmitter;
	private ConcurrentHashMap<String, Disposable> mapIdToDisposable = new ConcurrentHashMap<>();

	public Timers(EventEmitter eventEmitter) {
		this.eventEmitter = eventEmitter;
	}

    /**
     * To schedule execution of a one-time callback after delay milliseconds. Returns a timeoutId for possible use with clearTimeout().
     *
     * It is important to note that your callback will probably not be called in exactly <delay> ms - Nova makes no guarantees about the
     * exact timing of when the callback will fire, nor of the ordering things will fire in. The callback will be called as close as
     * possible to the time specified.
     */
	public String setTimeout(Runnable callback, long delay) {
	    return setTimeout(callback, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * To schedule execution of a one-time callback after a specified delay . Returns a timeoutId for possible use with clearTimeout().
     *
     * It is important to note that your callback will probably not be called in exactly <delay> - Nova makes no guarantees about the
     * exact timing of when the callback will fire, nor of the ordering things will fire in. The callback will be called as close as
     * possible to the time specified.
     */
	public String setTimeout(Runnable callback, long delay, TimeUnit timeUnit) {
		requireNonNull(callback, "callback must not be null");
		requireNonNull(timeUnit, "timeUnit must not be null");
        String newId = String.valueOf(counter.incrementAndGet());
        String timeoutDummyEvent = "TimersTimeoutDummyEvent" + newId;
        Disposable single = eventEmitter
                .single(timeoutDummyEvent)
                .delay(delay, timeUnit)
                .subscribe(x -> callback.run());;
        mapIdToDisposable.put(newId, single);
		eventEmitter.emit(timeoutDummyEvent);
		return newId;
	}

    /** Prevents the timeout with the passed ID from triggering. */
    public void clearTimeout(String timeoutId) {
        dispose(timeoutId);
    }

	/**
	 * To schedule the repeated execution of callback every interval milliseconds. Returns a intervalId for possible use with clearInterval().
	 *
	 */
	public String setInterval(Runnable callback, long interval) {
		return setInterval(callback, 0, interval, TimeUnit.MILLISECONDS);
	}

	/**
	 * To schedule the repeated execution of callback. Returns a intervalId for possible use with clearInterval().
	 *
	 */
	public String setInterval(Runnable callback, long initialDelay, long interval, TimeUnit timeUnit) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(timeUnit, "timeUnit must not be null");
        String newId = String.valueOf(counter.incrementAndGet());
        String intervalDummyEvent = "TimersIntervalDummyEvent" + newId;
        Disposable callbackInvoker = eventEmitter
                .observe(intervalDummyEvent)
                .subscribe(x -> callback.run());
        // we create another "wrapper Observable" to trigger the callback invovation over the EventEmitter
        // if we would invoke it from the Observable directly, we would be on the RxJava scheduler thread, not the
        // (user configured) Nova event handler thread(s)
        Disposable recurringEventEmitter = Observable
                .interval(initialDelay, interval, timeUnit)
                .subscribe(
                        counter -> eventEmitter.emit(intervalDummyEvent),
                        error -> logger.error("An error occurred on interval invocation for intervalId " + newId, error),
                        () -> callbackInvoker.dispose());
        mapIdToDisposable.put(newId, recurringEventEmitter);

        return newId;
	}

	/**
	 * Stops an interval from triggering.
	 */
	public void clearInterval(String intervalId) {
	    dispose(intervalId);
	}

	private void dispose (String disposableId) {
        requireNonNull(disposableId, "ID must not be null");
        Disposable disposable = mapIdToDisposable.remove(disposableId);
        if (disposable != null) {
            disposable.dispose();
        }
    }

}
