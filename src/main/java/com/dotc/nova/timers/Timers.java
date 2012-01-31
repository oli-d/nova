package com.dotc.nova.timers;

import java.util.concurrent.*;

import org.apache.log4j.Logger;

import com.dotc.nova.ProcessingLoop;

public class Timers {
	private static final Logger LOGGER = Logger.getLogger(Timers.class);

	private ProcessingLoop processingLoop;
	private final ScheduledExecutorService executor;
	private long counter = 0;
	private ConcurrentHashMap<String, ScheduledFuture> mapIdToFuture = new ConcurrentHashMap<String, ScheduledFuture>();

	public Timers() {
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

	public void setProcessingLoop(ProcessingLoop processingLoop) {
		this.processingLoop = processingLoop;
	}

	/**
	 * To schedule execution of a one-time callback after delay milliseconds. Returns a timeoutId for possible use with clearTimeout().
	 * 
	 * It is important to note that your callback will probably not be called in exactly delay milliseconds - Nova makes no guarantees about the exact timing of when the callback will fire, nor of the
	 * ordering things will fire in. The callback will be called as close as possible to the time specified.
	 */
	public String setTimeout(Runnable callback, long delay) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		long id = ++counter;
		String idAsString = String.valueOf(id);

		mapIdToFuture.put(idAsString, executor.schedule(new TimeoutCallbackWrapper(callback), delay, TimeUnit.MILLISECONDS));

		return idAsString;
	}

	/** Prevents the timeout with the passed ID from triggering. */
	public void clearTimeout(String timeoutId) {
		if (timeoutId == null) {
			throw new IllegalArgumentException("timeoutId must not be null");
		}
		ScheduledFuture sf = mapIdToFuture.get(timeoutId);
		if (sf != null) {
			sf.cancel(false);
		}
	}

	/**
	 * To schedule the repeated execution of callback every delay milliseconds. Returns a intervalId for possible use with clearInterval(). Optionally you can also pass arguments to the callback.
	 * 
	 */
	public String setInterval(Runnable callback, long delay) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		long id = ++counter;
		String idAsString = String.valueOf(id);

		mapIdToFuture.put(idAsString, executor.scheduleWithFixedDelay(new TimeoutCallbackWrapper(callback), delay, delay, TimeUnit.MILLISECONDS));

		return idAsString;
	}

	/**
	 * Stops a interval from triggering.
	 */
	public void clearInterval(String intervalId) {
		if (intervalId == null) {
			throw new IllegalArgumentException("timeoutId must not be null");
		}
		ScheduledFuture sf = mapIdToFuture.get(intervalId);
		if (sf != null) {
			sf.cancel(false);
		}
	}

	private class TimeoutCallbackWrapper implements Runnable {
		private final Runnable runnableToInvoke;

		public TimeoutCallbackWrapper(Runnable runnableToInvoke) {
			super();
			this.runnableToInvoke = runnableToInvoke;
		}

		@Override
		public void run() {
			try {
				processingLoop.dispatch(runnableToInvoke);
			} catch (Throwable t) {
				LOGGER.error("Unable to put callback on processing loop", t);
			}
		}

	}
}
