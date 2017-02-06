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

import ch.squaredesk.nova.Nova;
import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimersMemLeakTest {
	static {
		BasicConfigurator.configure();
	}

	private Timers sut;

	@Before
    public void setup() {
        Nova nova = Nova.builder().build();
		sut = nova.timers;
    }

	@Test
	public void setTimeoutLeavesNothingAfterItWasInvoked() throws Throwable {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Runnable callback = countDownLatch::countDown;
		Map<String, ScheduledFuture<?>> internalMap = getInternalFutureMapFrom(sut);

		assertTrue(internalMap.isEmpty());
		assertThat(countDownLatch.getCount(), Matchers.is(1L));
		sut.setTimeout(callback, 250, TimeUnit.MILLISECONDS);
		assertFalse(internalMap.isEmpty());

		countDownLatch.await(500, TimeUnit.MILLISECONDS);
		assertThat(countDownLatch.getCount(), Matchers.is(0L));

		assertTrue(internalMap.isEmpty());
	}

	@Test
	public void setTimeoutThatThrowsLeavesNothingAfterItWasInvoked() throws Throwable {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Runnable callback = () -> {
		        countDownLatch.countDown();
		        throw new RuntimeException("for test");
        };
		Map<String, ScheduledFuture<?>> internalMap = getInternalFutureMapFrom(sut);

		assertTrue(internalMap.isEmpty());
		assertThat(countDownLatch.getCount(), Matchers.is(1L));
		sut.setTimeout(callback, 250, TimeUnit.MILLISECONDS);
		assertFalse(internalMap.isEmpty());

		countDownLatch.await(500, TimeUnit.MILLISECONDS);
		assertThat(countDownLatch.getCount(), Matchers.is(0L));

		assertTrue(internalMap.isEmpty());
	}

	@Test
	public void clearTimeoutRemovesEverything() throws Throwable {
		Runnable callback = () -> {};
		Map<String, ScheduledFuture<?>> internalMap = getInternalFutureMapFrom(sut);

		assertTrue(internalMap.isEmpty());
		String id = sut.setTimeout(callback, 250, TimeUnit.MILLISECONDS);
		assertFalse(internalMap.isEmpty());
		sut.clearTimeout(id);
		assertTrue(internalMap.isEmpty());
	}

	@Test
	public void clearIntervalRemovesEverything() throws Throwable {
		Runnable callback = () -> {};
		Map<String, ScheduledFuture<?>> internalMap = getInternalFutureMapFrom(sut);

		assertTrue(internalMap.isEmpty());
		String id = sut.setInterval(callback,0, 250, TimeUnit.MILLISECONDS);
		assertFalse(internalMap.isEmpty());
		sut.clearTimeout(id);
		assertTrue(internalMap.isEmpty());
	}

	@Test
	public void intervalThatThrowsIsKilledAndRemovesEverything() throws Throwable {
		CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicInteger atomicInteger = new AtomicInteger(0);
	    Runnable callback = () -> {
	        if (atomicInteger.incrementAndGet() % 7 == 0) {
                countDownLatch.countDown();
                throw new RuntimeException("for test");
            }
		};
		Map<String, ScheduledFuture<?>> internalMap = getInternalFutureMapFrom(sut);

		assertTrue(internalMap.isEmpty());
		sut.setInterval(callback,0, 25, TimeUnit.MILLISECONDS);
		assertFalse(internalMap.isEmpty());
		countDownLatch.await(1, TimeUnit.SECONDS);
		assertThat(countDownLatch.getCount(), Matchers.is (0L));
		assertTrue(internalMap.isEmpty());
	}

	@SuppressWarnings("unchecked")
	private Map<String, ScheduledFuture<?>> getInternalFutureMapFrom(Timers timers) throws Exception {
		Field f = Timers.class.getDeclaredField("mapIdToDisposable");
		f.setAccessible(true);
		return (Map<String, ScheduledFuture<?>>) f.get(timers);
	}
}
