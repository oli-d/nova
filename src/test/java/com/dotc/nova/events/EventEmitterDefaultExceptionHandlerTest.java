package com.dotc.nova.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.dotc.nova.Nova;
import com.dotc.nova.events.EventDispatchConfig.DispatchThreadStrategy;
import com.dotc.nova.events.wrappers.SingleParameterEventListener;

@RunWith(MockitoJUnitRunner.class)
public class EventEmitterDefaultExceptionHandlerTest {
	private EventEmitter eventEmitter;

	@Before
	public void setup() {
		EventDispatchConfig edc = new EventDispatchConfig.Builder().setDispatchThreadStrategy(
				DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD).build();
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
