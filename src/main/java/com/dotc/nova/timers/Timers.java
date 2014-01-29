package com.dotc.nova.timers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dotc.nova.events.EventListener;
import com.dotc.nova.events.EventLoop;

public class Timers {
	private static final Logger LOGGER = LoggerFactory.getLogger(Timers.class);

	private final EventLoop eventLoop;
	private final ScheduledExecutorService executor;
	private long counter = 0;
	private ConcurrentHashMap<String, ScheduledFuture> mapIdToFuture = new ConcurrentHashMap<String, ScheduledFuture>();

	public Timers(EventLoop eventLoop) {
		this.eventLoop = eventLoop;

		ThreadFactory tf = new ThreadFactory() {

			@Override
			public Thread newThread(Runnable arg0) {
				Thread t = new Thread(arg0, "Timers");
				t.setDaemon(true);
				return t;
			}
		};
		executor = Executors.newSingleThreadScheduledExecutor(tf);
	}

	/**
	 * To schedule execution of a one-time callback after delay milliseconds. Returns a timeoutId for possible use with clearTimeout().
	 * 
	 * It is important to note that your callback will probably not be called in exactly delay milliseconds - Nova makes no guarantees about
	 * the exact timing of when the callback will fire, nor of the
	 * ordering things will fire in. The callback will be called as close as possible to the time specified.
	 */
	public String setTimeout(Runnable callback, long delay) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		long id = ++counter;
		String idAsString = String.valueOf(id);

		mapIdToFuture.put(idAsString, executor.schedule(new TimeoutCallbackWrapper(idAsString, callback), delay, TimeUnit.MILLISECONDS));

		return idAsString;
	}

	public String setTimeout(Runnable callback, long delay, TimeUnit timeUnit) {
		return setTimeout(callback, timeUnit.toMillis(delay));
	}

	/** Prevents the timeout with the passed ID from triggering. */
	public void clearTimeout(String timeoutId) {
		if (timeoutId == null) {
			throw new IllegalArgumentException("timeoutId must not be null");
		}
		ScheduledFuture sf = mapIdToFuture.remove(timeoutId);
		if (sf != null) {
			sf.cancel(false);
		}
	}

	/**
	 * To schedule the repeated execution of callback every delay milliseconds. Returns a intervalId for possible use with clearInterval().
	 * 
	 */
	public String setInterval(Runnable callback, long delay) {
		return setInterval(callback, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * To schedule the repeated execution of callback. Returns a intervalId for possible use with clearInterval().
	 * 
	 */
	public String setInterval(Runnable callback, long delay, TimeUnit timeUnit) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		if (timeUnit == null) {
			throw new IllegalArgumentException("timeUnit must not be null");
		}
		long id = ++counter;
		String idAsString = String.valueOf(id);

		mapIdToFuture.put(idAsString, executor.scheduleWithFixedDelay(new IntervalCallbackWrapper(callback), delay, delay, timeUnit));

		return idAsString;
	}

	/**
	 * Stops an interval from triggering.
	 */
	public void clearInterval(String intervalId) {
		if (intervalId == null) {
			throw new IllegalArgumentException("timeoutId must not be null");
		}
		ScheduledFuture sf = mapIdToFuture.remove(intervalId);
		if (sf != null) {
			sf.cancel(false);
		}
	}

	private class TimeoutCallbackWrapper implements Runnable {
		private final EventListener handlerToInvoke;

		public TimeoutCallbackWrapper(final String callbackId, final Runnable runnableToInvoke) {
			this.handlerToInvoke = new EventListener() {
				@Override
				public void handle(Object... data) {
					clearTimeout(callbackId);
					runnableToInvoke.run();
				}
			};
		}

		@Override
		public void run() {
			try {
				eventLoop.dispatch(handlerToInvoke);
			} catch (Throwable t) {
				LOGGER.error("Unable to put timeout callback on processing loop", t);
			}
		}

	}

	private class IntervalCallbackWrapper implements Runnable {
		private final EventListener handlerToInvoke;

		public IntervalCallbackWrapper(final Runnable runnableToInvoke) {
			this.handlerToInvoke = new EventListener() {
				@Override
				public void handle(Object... data) {
					runnableToInvoke.run();
				}
			};
		}

		@Override
		public void run() {
			try {
				eventLoop.dispatch(handlerToInvoke);
			} catch (Throwable t) {
				LOGGER.error("Unable to put interval callback on processing loop", t);
			}
		}

	}
}
