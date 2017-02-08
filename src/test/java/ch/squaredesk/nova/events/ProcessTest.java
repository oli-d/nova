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
import ch.squaredesk.nova.events.Process;
import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class ProcessTest {
	private Process process;
	private EventLoop eventLoop;

	@BeforeClass
	public static void initLogging() {
		BasicConfigurator.configure();
	}

	@Before
	public void setup() {
		Nova nova = Nova.builder().build();
		process = nova.process;
		eventLoop = nova.eventLoop;
	}

	@Test(expected = NullPointerException.class)
	public void testNextTickPassingNullThrows() {
		process.nextTick(null);
	}

	@Test
	public void testNextTickPutsCallbackOnProcessingLoop() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Runnable myCallback = () -> countDownLatch.countDown();

		process.nextTick(myCallback);

		countDownLatch.await(500, TimeUnit.MILLISECONDS);
		Assert.assertThat(countDownLatch.getCount(), Matchers.is(0L));
	}

	@Test
	public void nextTickThatThrowsDoesNotLeak() throws Throwable {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Runnable callback = () -> {
			countDownLatch.countDown();
			throw new RuntimeException("for test");
		};

		assertNull(eventLoop.subjectFor(Process.DUMMY_NEXT_TICK_EVENT_PREFIX + 1));
		process.nextTick(callback);
		countDownLatch.await(1, TimeUnit.SECONDS);
		assertThat(countDownLatch.getCount(), Matchers.is(0L));
		// unfortunately, the disposal does not run synchronously, so we have to wait a little
		Thread.sleep(100);
		assertNull(eventLoop.subjectFor(Process.DUMMY_NEXT_TICK_EVENT_PREFIX + 1));
	}


}
