/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.timers;

import ch.squaredesk.nova.events.EventDispatchConfig;
import ch.squaredesk.nova.events.EventLoop;
import ch.squaredesk.nova.events.metrics.NoopEventMetricsCollector;
import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TimersMemLeakTest {
	static {
		BasicConfigurator.configure();
	}

	@Test
	public void testSetTimeoutLeavesNothingAfterItWasInvoked() throws Throwable {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Runnable callback = countDownLatch::countDown;
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
				new NoopEventMetricsCollector());
		Timers timers = new Timers(eventLoop);
		Map<String, ScheduledFuture<?>> internalMap = getInternalFutureMapFrom(timers);

		assertTrue(internalMap.isEmpty());
		assertThat(countDownLatch.getCount(), Matchers.is(1L));
		timers.setTimeout(callback, 250, TimeUnit.MILLISECONDS);
		assertFalse(internalMap.isEmpty());

		countDownLatch.await(500, TimeUnit.MILLISECONDS);
		assertThat(countDownLatch.getCount(), Matchers.is(0L));

		assertTrue(internalMap.isEmpty());
	}

	@Test
	public void testClearTimeoutRemovesEverything() throws Throwable {
		Runnable callback = () -> {};
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
				new NoopEventMetricsCollector());
		Timers timers = new Timers(eventLoop);
		Map<String, ScheduledFuture<?>> internalMap = getInternalFutureMapFrom(timers);

		assertTrue(internalMap.isEmpty());
		String id = timers.setTimeout(callback, 250, TimeUnit.MILLISECONDS);
		assertFalse(internalMap.isEmpty());
		timers.clearTimeout(id);
		assertTrue(internalMap.isEmpty());
	}

	@Test
	public void testClearIntervalRemovesEverything() throws Throwable {
		Runnable callback = () -> {};
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
				new NoopEventMetricsCollector());
		Timers timers = new Timers(eventLoop);
		Map<String, ScheduledFuture<?>> internalMap = getInternalFutureMapFrom(timers);

		assertTrue(internalMap.isEmpty());
		String id = timers.setInterval(callback, 250, TimeUnit.MILLISECONDS);
		assertFalse(internalMap.isEmpty());
		timers.clearTimeout(id);
		assertTrue(internalMap.isEmpty());
	}

	@SuppressWarnings("unchecked")
	private Map<String, ScheduledFuture<?>> getInternalFutureMapFrom(Timers timers) throws Exception {
		Field f = Timers.class.getDeclaredField("mapIdToFuture");
		f.setAccessible(true);
		return (Map<String, ScheduledFuture<?>>) f.get(timers);
	}

}
