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
import ch.squaredesk.nova.events.consumers.SingleParameterConsumer;
import io.reactivex.Observable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EventEmitterDefaultExceptionHandlerTest {
	private EventEmitter eventEmitter;

	@Before
	public void setup() {
		EventDispatchConfig edc = EventDispatchConfig.builder().setDispatchThreadStrategy(
				EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD).build();
		eventEmitter = Nova.builder().setEventDispatchConfig(edc).build().eventEmitter;
	}

	@Test
	public void testEmitterDoesntDieOnUnhandledException() throws InterruptedException {
		final int[] counter = new int[1];
		final CountDownLatch latch = new CountDownLatch(1);

		SingleParameterConsumer<String> listener = param -> {
			if ("throw".equals(param)) {
				throw new RuntimeException("for test");
			} else if ("end".equals(param)) {
				latch.countDown();
			} else {
				counter[0]++;
			}
		};

        Observable observable = eventEmitter.observe("xxx");

        observable.subscribe(listener);
        eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "throw");
		observable.subscribe(listener);
		eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "end");

		latch.await(2, TimeUnit.SECONDS);

		Assert.assertEquals(4, counter[0]);
	}

	@Test
	public void testSubscriptionDiesOnUnhandledException() throws InterruptedException {
		final int[] counter = new int[1];
		final CountDownLatch latch = new CountDownLatch(1);

		SingleParameterConsumer<String> listener = param -> {
			if ("throw".equals(param)) {
				throw new RuntimeException("for test");
			} else if ("end".equals(param)) {
				latch.countDown();
			} else {
				counter[0]++;
			}
		};

        Observable observable = eventEmitter.observe("xxx");

        observable.subscribe(listener);
        eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "throw");
		eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "count");
		eventEmitter.emit("xxx", "end");

		latch.await(2, TimeUnit.SECONDS);

		Assert.assertEquals(2, counter[0]);
	}

}
