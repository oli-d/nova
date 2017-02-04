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

import ch.squaredesk.nova.events.metrics.NoopEventMetricsCollector;
import ch.squaredesk.nova.events.wrappers.NoParameterEventListener;
import ch.squaredesk.nova.events.wrappers.SingleParameterEventListener;
import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class EventLoopTest {
	/*
	private EventLoop eventLoop;

	@BeforeClass
	public static void setupLogging() {
		BasicConfigurator.configure();
	}

	@Before
	public void setUp() {
		eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
				new NoopEventMetricsCollector());
	}

	private EventListener[] createListeners(final CountDownLatch countDownLatch) {
		EventListener[] listenersArray = new EventListener[(int) countDownLatch.getCount()];
		for (int i = 0; i < listenersArray.length; i++) {
			listenersArray[i] = data -> countDownLatch.countDown();
		}
		return listenersArray;
	}

	@Test
	public void testDispatchEventWithListenerArray() {
		int numberOfListeners = 5;
		final CountDownLatch countDownLatch = new CountDownLatch(numberOfListeners);
		EventListener[] listenersArray = createListeners(countDownLatch);

		eventLoop.dispatch("Test", listenersArray);

		try {
			countDownLatch.await(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		assertThat(countDownLatch.getCount(), is(0L));
	}

	@Test
	public void testDispatchEventWithListenerList() {
		int numberOfListeners = 5;
		final CountDownLatch countDownLatch = new CountDownLatch(numberOfListeners);
		EventListener[] listenersArray = createListeners(countDownLatch);

		ArrayList<EventListener> list = new ArrayList<>(Arrays.asList(listenersArray));
		eventLoop.dispatch("Test", list, "Data");

		try {
			countDownLatch.await(10000, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		assertThat(countDownLatch.getCount(), is(0L));
	}

	@Test
	public void testDispatchHandlerWithoutEvent() {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		EventListener r = data -> countDownLatch.countDown();

		eventLoop.dispatch(r);

		try {
			countDownLatch.await(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}

		assertThat(countDownLatch.getCount(), is(0L));
	}

	@Test
	public void ensureNumberOfConsumersAreInstantiatedAccordingToSettingDispatchingEventsToAllConsumers() {
		int numberOfConsumers = 7;
		int numberOfEvents = 100000;

		eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().setEventBufferSize(numberOfEvents)
				.setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
				.setMultiConsumerDispatchStrategy(EventDispatchConfig.MultiConsumerDispatchStrategy.DISPATCH_EVENTS_TO_ALL_CONSUMERS)
				.setNumberOfConsumers(numberOfConsumers).build(),
                new NoopEventMetricsCollector());

		final CountDownLatch latch = new CountDownLatch(numberOfConsumers);
		final ConcurrentHashMap<Thread, Thread> threads = new ConcurrentHashMap<>();
		NoParameterEventListener threadDetectingListener = () -> threads.put(Thread.currentThread(),
				Thread.currentThread());
		NoParameterEventListener endListener = latch::countDown;

		for (int i = 0; i < numberOfEvents; i++) {
			eventLoop.dispatch("tick", threadDetectingListener);
		}
		eventLoop.dispatch("end", endListener);

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		org.junit.Assert.assertEquals(numberOfConsumers, threads.size());
	}

	@Test
	public void ensureNumberOfConsumersAreInstantiatedAccordingToSettingDispatchingEventsToOneConsumer() {
		int numberOfThreads = 7;
		int numberOfEvents = 100000;

		eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().setEventBufferSize(numberOfEvents)
				.setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
				.setMultiConsumerDispatchStrategy(EventDispatchConfig.MultiConsumerDispatchStrategy.DISPATCH_EVENTS_TO_ONE_CONSUMER)
				.setNumberOfConsumers(numberOfThreads).build(), new NoopEventMetricsCollector());

		final CountDownLatch latch = new CountDownLatch(1);
		final ConcurrentHashMap<Thread, Thread> threads = new ConcurrentHashMap<>();
		NoParameterEventListener threadDetectingListener = () -> threads.put(Thread.currentThread(),
				Thread.currentThread());
		NoParameterEventListener endListener = latch::countDown;

		for (int i = 0; i < numberOfEvents; i++) {
			eventLoop.dispatch("tick", threadDetectingListener);
		}
		eventLoop.dispatch("end", endListener);

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		org.junit.Assert.assertEquals(numberOfThreads, threads.size());
	}

	@Test
	public void testEventsAreDroppedIfQueueIsFullAndStrategySaysSo() {
		eventLoop = new EventLoop("test", new EventDispatchConfig.Builder()
		.setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
		.setInsufficientCapacityStrategy(EventDispatchConfig.InsufficientCapacityStrategy.DROP_EVENTS).setEventBufferSize(5)
		.build(), new NoopEventMetricsCollector());
		int numEvents = 1000000;
		final int[] numEventsProcessed = new int[1];

		NoParameterEventListener listener = () -> {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			numEventsProcessed[0]++;
		};

		for (int i = 0; i < numEvents; i++) {
			eventLoop.dispatch("bla", listener);
		}

		assertTrue(numEventsProcessed[0] < numEvents);
	}

	@Test
	public void testEventsAreQueuedInMemeoryIfQueueIsFullAndStrategySaysSo() {
		eventLoop = new EventLoop("test", new EventDispatchConfig.Builder()
		.setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
		.setInsufficientCapacityStrategy(EventDispatchConfig.InsufficientCapacityStrategy.QUEUE_EVENTS).setEventBufferSize(1)
		.build(), new NoopEventMetricsCollector());
		int numEvents = 3;
		final CountDownLatch latch = new CountDownLatch(numEvents);
		NoParameterEventListener listener = () -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			latch.countDown();
		};

		for (int i = 0; i < numEvents; i++) {
			eventLoop.dispatch("bla", listener);
		}

		try {
			latch.await(4250, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}

		assertEquals(0, latch.getCount());
	}

	@Test
	public void testNoDispatchingUntilSpaceAvailableIfQueueIsFullAndStrategySaysSo() {
		int eventBufferSize = 4;
		eventLoop = new EventLoop("test", new EventDispatchConfig.Builder()
		.setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
		.setNumberOfConsumers(eventBufferSize)
		.setInsufficientCapacityStrategy(EventDispatchConfig.InsufficientCapacityStrategy.WAIT_UNTIL_SPACE_AVAILABLE)
		.setEventBufferSize(eventBufferSize).build(), new NoopEventMetricsCollector());
		final int numEvents = 100;
		final CountDownLatch initialBlockingLatch = new CountDownLatch(1);
		final AtomicInteger emitCountingInteger = new AtomicInteger();
		final AtomicInteger initialCountingInteger = new AtomicInteger();
		final CountDownLatch countingLatch = new CountDownLatch(numEvents);
		final NoParameterEventListener listener = () -> {
			initialCountingInteger.incrementAndGet();
			try {
				initialBlockingLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			countingLatch.countDown();
		};

		// start producer in a separate thread
		new Thread() {
			@Override
			public void run() {
				for (int i = 0; i < numEvents; i++) {
					eventLoop.dispatch("bla", listener);
					emitCountingInteger.getAndIncrement();
				}
			}
		}.start();

		long endTime = System.currentTimeMillis() + 1750;
		while (initialCountingInteger.intValue() < eventBufferSize && System.currentTimeMillis() < endTime) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// assert that all buffer places were filled
		assertEquals(eventBufferSize, initialCountingInteger.intValue());
		assertEquals(eventBufferSize, emitCountingInteger.intValue());
		assertEquals(numEvents, countingLatch.getCount());
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// and nothing happens at the moment
		assertEquals(eventBufferSize, initialCountingInteger.intValue());
		assertEquals(eventBufferSize, emitCountingInteger.intValue());
		assertEquals(numEvents, countingLatch.getCount());

		// let it rock
		initialBlockingLatch.countDown();

		// wait for all events to be processed
		try {
			countingLatch.await(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(0, countingLatch.getCount());
	}

	@Test
	public void testOutdatedEventsAreDroppedIfTheyAreProducedFasterThanConsumed() throws InterruptedException {
		int numEvents = 1000000;
		eventLoop = new EventLoop("test", new EventDispatchConfig.Builder()
		.setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
		.setEventBufferSize(numEvents).build(), new NoopEventMetricsCollector());
		eventLoop.registerIdProviderForDuplicateEventDetection("bla", data -> "id");
		final int[] numEventsProcessed = new int[1];
		final String[] lastDataProcessed = new String[1];

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		SingleParameterEventListener<String> listener = data -> {
			if ("bye".equals(data)) {
				countDownLatch.countDown();
				return;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			numEventsProcessed[0]++;
			lastDataProcessed[0] = data;
		};

		for (int i = 0; i < numEvents; i++) {
			eventLoop.dispatch("bla", listener, "value" + i);
		}
		eventLoop.dispatch("blo", listener, "bye");

		countDownLatch.await(2, TimeUnit.SECONDS);

		assertTrue(numEventsProcessed[0] < numEvents);
		assertThat(lastDataProcessed[0], Matchers.is("value" + (numEvents - 1)));
	}
*/
}
