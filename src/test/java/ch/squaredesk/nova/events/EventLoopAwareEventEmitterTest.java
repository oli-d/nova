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

import ch.squaredesk.nova.events.consumers.NoParameterConsumer;
import ch.squaredesk.nova.events.consumers.SingleParameterConsumer;
import ch.squaredesk.nova.metrics.Metrics;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class EventLoopAwareEventEmitterTest extends EventEmitterTestBase {

	@Override
	public EventLoopAwareEventEmitter createEventEmitter() {
		return createEventEmitter(EventDispatchConfig.builder()
                .setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD)
				.build());
	}

	@Override
	protected EventLoopAwareEventEmitter createEventEmitter(EventDispatchConfig eventDispatchConfig) {
		return new EventLoopAwareEventEmitter("id", eventDispatchConfig, new Metrics());
	}

	@Test
	public void ensureNumberOfConsumersAreInstantiatedAccordingToSettings() {
		int numberOfThreads = 7;
		int numberOfEvents = 100000;

		EventDispatchConfig eventDispatchConfig = EventDispatchConfig.builder()
                .setEventBufferSize(numberOfEvents)
				.setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
				.setNumberOfConsumers(numberOfThreads).build();
		EventEmitter ee = createEventEmitter(eventDispatchConfig);

		final CountDownLatch latch = new CountDownLatch(1);
		final ConcurrentHashMap<Thread, Thread> threads = new ConcurrentHashMap<>();
		NoParameterConsumer threadDetectingConsumer = () -> threads.put(Thread.currentThread(), Thread.currentThread());
		ee.observe("tick").subscribe(threadDetectingConsumer);
		NoParameterConsumer endConsumer = latch::countDown;
		ee.observe("end").subscribe(endConsumer);

		for (int i = 0; i < numberOfEvents; i++) {
			ee.emit("tick");
		}
		ee.emit("end");

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(numberOfThreads, threads.size());
	}

    @Test
    public void testEventsAreDroppedIfQueueIsFullAndStrategySaysSo() {
        EventEmitter ee = createEventEmitter(EventDispatchConfig.builder()
                .setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
                .setInsufficientCapacityStrategy(EventDispatchConfig.InsufficientCapacityStrategy.DROP_EVENTS)
                .setEventBufferSize(5)
                .build());
        int numEvents = 1000000;
        final int[] numEventsProcessed = new int[1];

        NoParameterConsumer consumer = () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            numEventsProcessed[0]++;
        };
        ee.observe("bla").subscribe(consumer);

        for (int i = 0; i < numEvents; i++) {
            ee.emit("bla");
        }

        assertTrue(numEventsProcessed[0] < numEvents);
    }

    @Test
    public void testEventsAreQueuedInMemoryIfQueueIsFullAndStrategySaysSo() {
        EventEmitter ee = createEventEmitter(EventDispatchConfig.builder()
                .setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
                .setInsufficientCapacityStrategy(EventDispatchConfig.InsufficientCapacityStrategy.DROP_EVENTS)
                .setEventBufferSize(5)
                .build());
        int numEvents = 3;
        final CountDownLatch latch = new CountDownLatch(numEvents);
        NoParameterConsumer consumer = () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();
        };
        ee.observe("bla").subscribe(consumer);

        for (int i = 0; i < numEvents; i++) {
            ee.emit("bla");
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
        EventEmitter ee = createEventEmitter(EventDispatchConfig.builder()
                .setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
                .setInsufficientCapacityStrategy(EventDispatchConfig.InsufficientCapacityStrategy.WAIT_UNTIL_SPACE_AVAILABLE)
                .setEventBufferSize(eventBufferSize)
                .build());
        final int numEvents = 100;
        final CountDownLatch initialBlockingLatch = new CountDownLatch(1);
        final AtomicInteger emitCountingInteger = new AtomicInteger();
        final CountDownLatch countingLatch = new CountDownLatch(numEvents);
        final NoParameterConsumer consumer = () -> {
            try {
                initialBlockingLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            countingLatch.countDown();
        };
        ee.observe("bla").subscribe(consumer);

        // start producer in a separate thread
        new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < numEvents; i++) {
                    ee.emit("bla");
                    emitCountingInteger.getAndIncrement();
                }
            }
        }.start();

        // fill the ring buffer
        long endTime = System.currentTimeMillis() + 1750;
        while (emitCountingInteger.intValue() < eventBufferSize && System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // assert that all buffer places were filled
        assertThat(emitCountingInteger.intValue(),is(eventBufferSize));
        // and none consumed yet
        assertThat(countingLatch.getCount(), is((long)numEvents));
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // after a little wait, nothing should have been processed so far
        assertThat(emitCountingInteger.intValue(), is(eventBufferSize));
        assertThat(countingLatch.getCount(), is((long) numEvents));

        // let it rock
        initialBlockingLatch.countDown();

        // wait for all events to be processed
        try {
            countingLatch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertThat(countingLatch.getCount(), is(0L));
    }

    @Test
    public void testOutdatedEventsAreDroppedIfTheyAreProducedFasterThanConsumed() throws InterruptedException {
        int numEvents = 1000000;
        EventLoopAwareEventEmitter ee = createEventEmitter(EventDispatchConfig.builder()
                .setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
                .setEventBufferSize(numEvents)
                .build());
        ee.registerIdProviderForDuplicateEventDetection("bla", data -> "id");
        final int[] numEventsProcessed = new int[1];
        final String[] lastDataProcessed = new String[1];

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        SingleParameterConsumer<String> consumer = data -> {
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
        ee.observe("bla").subscribe(consumer);
        ee.single("blo").subscribe(consumer);

        for (int i = 0; i < numEvents; i++) {
            ee.emit("bla", "value" + i);
        }
        ee.emit("blo", "bye");

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(), is(0L));

        assertTrue(numEventsProcessed[0] < numEvents);
        assertThat(lastDataProcessed[0], is("value" + (numEvents - 1)));
    }

}
