package com.dotc.nova.events;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotc.nova.events.EventDispatchConfig.DispatchThreadStrategy;
import com.dotc.nova.events.EventDispatchConfig.InsufficientCapacityStrategy;
import com.dotc.nova.events.EventDispatchConfig.MultiConsumerDispatchStrategy;
import com.dotc.nova.events.metrics.NoopEventMetricsCollector;
import com.dotc.nova.events.wrappers.NoParameterEventListener;
import com.dotc.nova.events.wrappers.SingleParameterEventListener;

public class EventLoopTest {
	private EventLoop eventLoop;

	@BeforeClass
	public static void setupLogging() {
		BasicConfigurator.configure();
	}

	@Before
	public void setUp() {
		eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(), new NoopEventMetricsCollector());
	}

	private EventListener<String>[] createListeners(final CountDownLatch countDownLatch) {
		@SuppressWarnings("unchecked")
		EventListener<String>[] listenersArray = new EventListener[(int) countDownLatch.getCount()];
		for (int i = 0; i < listenersArray.length; i++) {
			listenersArray[i] = new EventListener<String>() {

				@Override
				public void handle(String... data) {
					countDownLatch.countDown();
				}
			};
		}
		return listenersArray;
	}

	@Test
	public void testDispatchEventWithListenerArray() {
		int numberOfListeners = 5;
		final CountDownLatch countDownLatch = new CountDownLatch(numberOfListeners);
		EventListener<String>[] listenersArray = createListeners(countDownLatch);

		eventLoop.dispatch("Test", listenersArray);

		try {
			countDownLatch.await(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		assertThat(countDownLatch.getCount(), is(0l));
	}

	@Test
	public void testDispatchEventWithListenerList() {
		int numberOfListeners = 5;
		final CountDownLatch countDownLatch = new CountDownLatch(numberOfListeners);
		EventListener<String>[] listenersArray = createListeners(countDownLatch);

		ArrayList<EventListener> list = new ArrayList<EventListener>(Arrays.asList(listenersArray));
		eventLoop.dispatch("Test", list, "Data");

		try {
			countDownLatch.await(10000, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		assertThat(countDownLatch.getCount(), is(0l));
	}

	@Test
	public void testDispatchHandlerWithoutEvent() {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		EventListener r = new EventListener() {
			@Override
			public void handle(Object... data) {
				countDownLatch.countDown();
			}
		};

		eventLoop.dispatch(r);

		try {
			countDownLatch.await(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}

		assertThat(countDownLatch.getCount(), is(0l));
	}

	@Test
	public void ensureNumberOfConsumersAreInstantiatedAccordingToSettingDispatchingEventsToAllConsumers() {
		int numberOfConsumers = 7;
		int numberOfEvents = 100000;

		eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().setEventBufferSize(numberOfEvents)
				.setDispatchThreadStrategy(DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
				.setMultiConsumerDispatchStrategy(MultiConsumerDispatchStrategy.DISPATCH_EVENTS_TO_ALL_CONSUMERS)
				.setNumberOfConsumers(numberOfConsumers).build(), new NoopEventMetricsCollector());

		final CountDownLatch latch = new CountDownLatch(numberOfConsumers);
		final ConcurrentHashMap<Thread, Thread> threads = new ConcurrentHashMap<>();
		NoParameterEventListener threadDetectingListener = new NoParameterEventListener() {
			@Override
			public void handle() {
				threads.put(Thread.currentThread(), Thread.currentThread());
			}
		};
		NoParameterEventListener endListener = new NoParameterEventListener() {
			@Override
			public void handle() {
				latch.countDown();
			}
		};

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
				.setDispatchThreadStrategy(DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
				.setMultiConsumerDispatchStrategy(MultiConsumerDispatchStrategy.DISPATCH_EVENTS_TO_ONE_CONSUMER)
				.setNumberOfConsumers(numberOfThreads).build(), new NoopEventMetricsCollector());

		final CountDownLatch latch = new CountDownLatch(1);
		final ConcurrentHashMap<Thread, Thread> threads = new ConcurrentHashMap<>();
		NoParameterEventListener threadDetectingListener = new NoParameterEventListener() {
			@Override
			public void handle() {
				threads.put(Thread.currentThread(), Thread.currentThread());
			}
		};
		NoParameterEventListener endListener = new NoParameterEventListener() {
			@Override
			public void handle() {
				latch.countDown();
			}
		};

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
		.setDispatchThreadStrategy(DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
		.setInsufficientCapacityStrategy(InsufficientCapacityStrategy.DROP_EVENTS).setEventBufferSize(5).build(),
				new NoopEventMetricsCollector());
		int numEvents = 1000000;
		final int[] numEventsProcessed = new int[1];

		EventListener listener = new NoParameterEventListener() {
			@Override
			public void handle() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				numEventsProcessed[0]++;
			}
		};

		for (int i = 0; i < numEvents; i++) {
			eventLoop.dispatch("bla", listener);
		}

		assertTrue(numEventsProcessed[0] < numEvents);
	}

	@Test
	public void testEventsAreQueuedInMemeoryIfQueueIsFullAndStrategySaysSo() {
		eventLoop = new EventLoop("test", new EventDispatchConfig.Builder()
		.setDispatchThreadStrategy(DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
		.setInsufficientCapacityStrategy(InsufficientCapacityStrategy.QUEUE_EVENTS).setEventBufferSize(1).build(),
				new NoopEventMetricsCollector());
		int numEvents = 3;
		final CountDownLatch latch = new CountDownLatch(numEvents);
		EventListener listener = new NoParameterEventListener() {
			@Override
			public void handle() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				latch.countDown();
			}
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
		.setDispatchThreadStrategy(DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD).setNumberOfConsumers(eventBufferSize)
		.setInsufficientCapacityStrategy(InsufficientCapacityStrategy.WAIT_UNTIL_SPACE_AVAILABLE)
		.setEventBufferSize(eventBufferSize).build(), new NoopEventMetricsCollector());
		final int numEvents = 100;
		final CountDownLatch initialBlockingLatch = new CountDownLatch(1);
		final AtomicInteger emitCountingInteger = new AtomicInteger();
		final AtomicInteger initialCountingInteger = new AtomicInteger();
		final CountDownLatch countingLatch = new CountDownLatch(numEvents);
		final EventListener listener = new NoParameterEventListener() {
			@Override
			public void handle() {
				initialCountingInteger.incrementAndGet();
				try {
					initialBlockingLatch.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				countingLatch.countDown();
			}
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
		.setDispatchThreadStrategy(DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD).setEventBufferSize(numEvents).build(),
				new NoopEventMetricsCollector());
		eventLoop.registerIdProviderForDuplicateEventDetection("bla", new IdProviderForDuplicateEventDetection() {
			@Override
			public Object provideIdFor(Object... data) {
				return "id";
			}
		});
		final int[] numEventsProcessed = new int[1];
		final String[] lastDataProcessed = new String[1];

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		EventListener listener = new SingleParameterEventListener<String>() {
			@Override
			public void handle(String data) {
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
			}
		};

		for (int i = 0; i < numEvents; i++) {
			eventLoop.dispatch("bla", listener, "value" + i);
		}
		eventLoop.dispatch("blo", listener, "bye");

		countDownLatch.await(2, TimeUnit.SECONDS);

		assertTrue(numEventsProcessed[0] < numEvents);
		assertThat(lastDataProcessed[0], Matchers.is("value" + (numEvents - 1)));
	}

	private void setIdToCurrentDataMap(EventLoop eventLoop, Map<Object, Object[]> mapIdToCurrentValue) throws Exception {
		Field f = EventLoop.class.getDeclaredField("mapIdToCurrentData");
		f.setAccessible(true);
		f.set(eventLoop, mapIdToCurrentValue);
	}

	// @Test
	// public void testThatAllListenerInvokedIfNoDuplicateRegistered() throws Exception {
	// Map<Object, Object[]> mapIdToCurrentValue = mock(Map.class);
	// setIdToCurrentDataMap(eventLoop, mapIdToCurrentValue);
	//
	// Object key = "key";
	// when(mapIdToCurrentValue.get(key)).thenReturn(null);
	// }
	//
	// @Test
	// public void testThatAllListenerInvokedIfNoDuplicateRegistered() throws Exception {
}
