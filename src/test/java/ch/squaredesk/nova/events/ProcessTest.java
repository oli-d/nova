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
	private EventLoop eventLoop;

	@BeforeClass
	public static void initLogging() {
		BasicConfigurator.configure();
	}

	@Before
	public void setup() {
		eventLoop = Nova.builder().build().eventLoop;
	}

	@Test(expected = NullPointerException.class)
	public void testNextTickPassingNullThrows() {
		eventLoop.nextTick(null);
	}

	@Test
	public void testNextTickPutsCallbackOnProcessingLoop() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Runnable myCallback = () -> countDownLatch.countDown();

		eventLoop.nextTick(myCallback);

		countDownLatch.await(500, TimeUnit.MILLISECONDS);
		Assert.assertThat(countDownLatch.getCount(), Matchers.is(0L));
	}


}
