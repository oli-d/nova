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
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class TimersTest {
    private Timers sut;

    @Before
    public void setup() {
        Nova nova = Nova.builder().build();
        sut = nova.timers;
    }

    @Test(expected = NullPointerException.class)
	public void testSetTimeoutThrowsIfNoCallbackProvided() {
		sut.setTimeout(null, 100);
	}

	@Test
	public void testSetTimeout() throws Throwable {
		BasicConfigurator.configure();

		CountDownLatch countDownLatch = new CountDownLatch(1);
		boolean[] invocationFlag = new boolean[1];
		Runnable callback = () -> {
			invocationFlag[0] = true;
			countDownLatch.countDown();
		};

		long startDelay = 300;
		assertNotNull(sut.setTimeout(callback, startDelay, TimeUnit.MILLISECONDS));

		countDownLatch.await(500, TimeUnit.MILLISECONDS);
		assertThat(invocationFlag[0],is(true));
	}

	@Test
	public void testClearTimeout() throws Exception {
        AtomicInteger counter = new AtomicInteger();

        Runnable callback = () -> counter.incrementAndGet();

		long startDelay = 200;
		String id = sut.setTimeout(callback, startDelay);
		sut.clearTimeout(id);
		Thread.sleep(2 * startDelay);
		assertNotNull(id);

		assertThat(counter.intValue(), is(0));
	}

	@Test(expected = NullPointerException.class)
	public void testClearTimeoutWithNullIdThrows() throws Exception {
		new Timers(null).clearTimeout(null);
	}

	@Test
	public void testClearTimeoutCanBeInvokedWithoutProblemsWithUnknownId() throws Exception {
		new Timers(null).clearTimeout("id");
	}

	@Test
	public void testClearTimeoutCanBeInvokedMultipleTimes() throws Exception {
        AtomicInteger counter = new AtomicInteger();

		long startDelay = 200;
		String id = sut.setTimeout(() -> counter.incrementAndGet(), startDelay);
		sut.clearTimeout(id);
		sut.clearTimeout(id);
		sut.clearTimeout(id);
		Thread.sleep(2 * startDelay);
		assertNotNull(id);

		assertThat(counter.intValue(), is(0));
	}

	@Test(expected = NullPointerException.class)
	public void testSetIntervalThrowsIfNoCallbackProvided() {
		sut.setInterval(null, 100);
	}

	@Test(expected = NullPointerException.class)
	public void testSetIntervalThrowsIfNoTimeUnitProvided() {
		sut.setInterval(() -> { },0, 100L, null);
	}

	@Test
	public void testSetInterval() throws Throwable {
        AtomicInteger counter = new AtomicInteger();

		long startDelay = 200;
		String id = sut.setInterval(() -> counter.incrementAndGet(), startDelay, startDelay, TimeUnit.MILLISECONDS);
		assertNotNull(id);
		Thread.sleep((4 * startDelay) + 100);
		sut.clearInterval(id);

		assertThat(counter.intValue(), is(4));
	}

	@Test
	public void testClearInterval() throws Exception {
        AtomicInteger counter = new AtomicInteger();

		long startDelay = 200;
		String id = sut.setInterval(()->counter.incrementAndGet(), startDelay);
		sut.clearInterval(id);
		Thread.sleep(2 * startDelay);
		assertNotNull(id);

		assertThat(counter.intValue(), is(0));
	}

	@Test(expected = NullPointerException.class)
	public void testClearIntervalWithNullIdThrows() throws Exception {
		sut.clearInterval(null);
	}

	@Test
	public void testClearIntervalCanBeInvokedWithoutProblemsWithUnknownId() throws Exception {
		sut.clearInterval("id");
	}

	@Test
	public void testClearIntervalCanBeInvokedMultipleTimes() throws Exception {
        AtomicInteger counter = new AtomicInteger();

		long startDelay = 200;
		String id = sut.setInterval(()->counter.incrementAndGet(), startDelay, startDelay, TimeUnit.MILLISECONDS);
		sut.clearInterval(id);
		sut.clearInterval(id);
		sut.clearInterval(id);
		Thread.sleep(2 * startDelay);
		assertNotNull(id);

		assertThat(counter.intValue(), is(0));
	}
}
