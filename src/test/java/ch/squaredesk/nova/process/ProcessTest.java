/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.process;

import ch.squaredesk.nova.Nova;
import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ProcessTest {
	private Process process;

	@BeforeClass
	public static void initLogging() {
		BasicConfigurator.configure();
	}

	@Before
	public void setup() {
		process = new Nova.Builder().build().process;
	}

	@Test(expected = IllegalArgumentException.class)
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
}
