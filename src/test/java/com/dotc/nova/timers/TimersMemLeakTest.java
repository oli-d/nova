package com.dotc.nova.timers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import com.dotc.nova.events.EventDispatchConfig;
import com.dotc.nova.events.EventLoop;

public class TimersMemLeakTest {
	static {
		BasicConfigurator.configure();
	}

	@Test
	public void testSetTimeoutLeavesNothingAfterItWasInvoked() throws Throwable {

		Runnable callback = mock(Runnable.class);
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build());
		Timers timers = new Timers(eventLoop);
		Map<String, ScheduledFuture> internalMap = getInternalFutureMapFrom(timers);

		assertTrue(internalMap.isEmpty());
		timers.setTimeout(callback, 250, TimeUnit.MILLISECONDS);
		assertFalse(internalMap.isEmpty());

		verify(callback, timeout(500)).run();

		assertTrue(internalMap.isEmpty());
	}

	@Test
	public void testClearTimeoutRemovesEverything() throws Throwable {
		Runnable callback = mock(Runnable.class);
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build());
		Timers timers = new Timers(eventLoop);
		Map<String, ScheduledFuture> internalMap = getInternalFutureMapFrom(timers);

		assertTrue(internalMap.isEmpty());
		String id = timers.setTimeout(callback, 250, TimeUnit.MILLISECONDS);
		assertFalse(internalMap.isEmpty());
		timers.clearTimeout(id);
		assertTrue(internalMap.isEmpty());
	}

	@Test
	public void testClearIntervalRemovesEverything() throws Throwable {
		Runnable callback = mock(Runnable.class);
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build());
		Timers timers = new Timers(eventLoop);
		Map<String, ScheduledFuture> internalMap = getInternalFutureMapFrom(timers);

		assertTrue(internalMap.isEmpty());
		String id = timers.setInterval(callback, 250, TimeUnit.MILLISECONDS);
		assertFalse(internalMap.isEmpty());
		timers.clearTimeout(id);
		assertTrue(internalMap.isEmpty());
	}

	private Map<String, ScheduledFuture> getInternalFutureMapFrom(Timers timers) throws Exception {
		Field f = Timers.class.getDeclaredField("mapIdToFuture");
		f.setAccessible(true);
		return (Map<String, ScheduledFuture>) f.get(timers);
	}

}
