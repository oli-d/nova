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

import ch.squaredesk.nova.Nova;
import io.reactivex.disposables.Disposable;
import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EventLoopMemLeakTest {
	static {
		BasicConfigurator.configure();
	}

	private EventLoop sut;

	@Before
    public void setup() {
		sut = Nova.builder().build().eventLoop;
    }

	@Test
	public void setTimeoutLeavesNothingAfterItWasInvoked() throws Throwable {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Runnable callback = countDownLatch::countDown;

		assertNull(sut.subjectFor(EventLoop.DUMMY_TIMEOUT_EVENT_PREFIX +1));
		assertThat(countDownLatch.getCount(), Matchers.is(1L));
		sut.setTimeout(callback, 250, TimeUnit.MILLISECONDS);
        assertNotNull(sut.subjectFor(EventLoop.DUMMY_TIMEOUT_EVENT_PREFIX +1));

		countDownLatch.await(500, TimeUnit.MILLISECONDS);
		assertThat(countDownLatch.getCount(), Matchers.is(0L));

        assertNull(sut.subjectFor(EventLoop.DUMMY_TIMEOUT_EVENT_PREFIX +1));
	}

	@Test
	public void setTimeoutThatThrowsLeavesNothingAfterItWasInvoked() throws Throwable {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Runnable callback = () -> {
		        countDownLatch.countDown();
		        throw new RuntimeException("for test");
        };

        assertNull(sut.subjectFor(EventLoop.DUMMY_TIMEOUT_EVENT_PREFIX +1));
		assertThat(countDownLatch.getCount(), Matchers.is(1L));
		sut.setTimeout(callback, 250, TimeUnit.MILLISECONDS);
        assertNotNull(sut.subjectFor(EventLoop.DUMMY_TIMEOUT_EVENT_PREFIX +1));

		countDownLatch.await(500, TimeUnit.MILLISECONDS);
		assertThat(countDownLatch.getCount(), Matchers.is(0L));

        assertNull(sut.subjectFor(EventLoop.DUMMY_TIMEOUT_EVENT_PREFIX +1));
	}

	@Test
	public void clearTimeoutRemovesEverything() throws Throwable {
		Runnable callback = () -> {};

        assertNull(sut.subjectFor(EventLoop.DUMMY_TIMEOUT_EVENT_PREFIX +1));
		Disposable disposable = sut.setTimeout(callback, 250, TimeUnit.MILLISECONDS);
        assertNotNull(sut.subjectFor(EventLoop.DUMMY_TIMEOUT_EVENT_PREFIX +1));
		disposable.dispose();
        assertNull(sut.subjectFor(EventLoop.DUMMY_TIMEOUT_EVENT_PREFIX +1));
	}

	@Test
	public void clearIntervalRemovesEverything() throws Throwable {
		Runnable callback = () -> {};

        assertNull(sut.subjectFor(EventLoop.DUMMY_INTERVAL_EVENT_PREFIX +1));
		Disposable disposable = sut.setInterval(callback,0, 250, TimeUnit.MILLISECONDS);
        assertNotNull(sut.subjectFor(EventLoop.DUMMY_INTERVAL_EVENT_PREFIX +1));
		disposable.dispose();

		assertNull(sut.subjectFor(EventLoop.DUMMY_INTERVAL_EVENT_PREFIX +1));


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

        assertNull(sut.subjectFor(EventLoop.DUMMY_INTERVAL_EVENT_PREFIX + 1));
        sut.setInterval(callback, 0, 155, TimeUnit.MILLISECONDS);
        assertNotNull(sut.subjectFor(EventLoop.DUMMY_INTERVAL_EVENT_PREFIX + 1));
        countDownLatch.await(1, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(), Matchers.is(0L));
        // unfortunately, the disposal does not run synchronously, so we have to wait a little
        Thread.sleep(100);
        assertNull(sut.subjectFor(EventLoop.DUMMY_INTERVAL_EVENT_PREFIX + 1));
	}

}
