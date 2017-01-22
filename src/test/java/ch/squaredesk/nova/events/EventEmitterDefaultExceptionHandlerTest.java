/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.events.wrappers.SingleParameterEventListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EventEmitterDefaultExceptionHandlerTest {
	private EventEmitter eventEmitter;

	@Before
	public void setup() {
		EventDispatchConfig edc = new EventDispatchConfig.Builder().setDispatchThreadStrategy(
				EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD).build();
		eventEmitter = new Nova.Builder().setEventDispatchConfig(edc).build().eventEmitter;
	}

	@Test
	public void testEmitterDoesntDieOnUnhandledException() throws InterruptedException {
		final int[] counter = new int[1];
		final CountDownLatch latch = new CountDownLatch(1);

		SingleParameterEventListener<String> listener = param -> {
			if ("throw".equals(param)) {
				throw new RuntimeException("for test");
			} else if ("end".equals(param)) {
				latch.countDown();
			} else {
				counter[0]++;
			}
		};

		eventEmitter.on("xxx", listener);

		eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "throw");
		eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "end");

		latch.await(2, TimeUnit.SECONDS);

		Assert.assertEquals(4, counter[0]);
	}
}
