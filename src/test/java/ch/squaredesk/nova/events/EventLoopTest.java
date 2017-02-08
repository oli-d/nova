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
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.Subject;
import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public abstract class EventLoopTest {
    private EventLoop eventLoop;

    @Before
    public void setup() {
        eventLoop = new EventLoop(
                "test",
                EventLoopConfig.builder().setDispatchThreadStrategy(EventLoopConfig.DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD).build(),
                new Metrics());
    }

    @BeforeClass
    public static void initLogging() {
        BasicConfigurator.configure();
    }

    @Test(expected = NullPointerException.class)
    public void testRegisteringNullEventThrows() {
        eventLoop.observe(null);
    }


    @Test
    public void unsubscribingLastObserverUnregistersListener() {
        Disposable d = eventLoop.observe("x").subscribe(data -> { });
        assertNotNull(eventLoop.subjectFor("x"));
        d.dispose();
        assertNull(eventLoop.subjectFor("x"));
    }

    @Test
    public void uncaughtErrorInSubscriberVoidsObservable() {
        AtomicInteger i = new AtomicInteger(0);

        Disposable d = eventLoop.observe("x")
                .subscribe(
                        data -> {
                            if (i.incrementAndGet()==2)
                            throw new RuntimeException("for test");
                        },
                        t -> {});
        assertNotNull(eventLoop.subjectFor("x"));
        assertFalse(d.isDisposed());

        eventLoop.emit("x");
        eventLoop.emit("x"); // should throw
        assertTrue(d.isDisposed());
        assertNotNull(eventLoop.subjectFor("x"));

        eventLoop.emit("x");
        assertTrue(d.isDisposed());
        assertThat(i.get(), is(2)); // 3rd one should not have been deliverd
    }


    @Test
    public void observableWithUncaughtErrorCannotProperlyBeDisposed() {
        Disposable d = eventLoop.observe("x")
                .subscribe(
                        data -> {
                            throw new RuntimeException("for test");
                        },
                        t -> {});
        assertNotNull(eventLoop.subjectFor("x"));
        assertFalse(d.isDisposed());

        eventLoop.emit("x"); // throws

        assertTrue(d.isDisposed());
        assertNotNull(eventLoop.subjectFor("x"));

        d.dispose();
        assertTrue(d.isDisposed());
        assertNotNull(eventLoop.subjectFor("x"));
    }

    @Test
    public void listenerIsLazilyRegistered() {
        Flowable f = eventLoop.observe("x");
        assertNull(eventLoop.subjectFor("x"));
        f.subscribe(data -> {});
        assertNotNull(eventLoop.subjectFor("x"));
    }

    @Test
    public void eachSubscriptionTriggeredBySameListener() {
        Flowable f = eventLoop.observe("x");
        assertNull(eventLoop.subjectFor("x"));
        f.subscribe(data -> {});
        Subject<Object[]> consumer = eventLoop.subjectFor("x");
        assertNotNull(consumer);
        f.subscribe(data -> {});
        Subject<Object[]> consumer2 = eventLoop.subjectFor("x");
        assertTrue(consumer==consumer2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmmittingNullThrows() {
        eventLoop.emit(null);
    }

    @Test
    public void testRegisteredListenerCalledEverytimeAnEventIsEmitted() throws Exception {
        List<Object[]> listener1InvocationParams = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(3);

        eventLoop.observe(String.class).subscribe(data -> {
            listener1InvocationParams.add(data);
            countDownLatch.countDown();
        });

        eventLoop.emit(String.class, "MyEvent1");
        eventLoop.emit(String.class, "MyEvent2");
        eventLoop.emit(String.class, "MyEvent3", "MyEvent4");

        countDownLatch.await(500,TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount(), is(0L));
        assertThat(listener1InvocationParams.size(), is(3));
        assertThat(listener1InvocationParams.get(0).length, is(1));
        assertThat(listener1InvocationParams.get(0)[0], is("MyEvent1"));
        assertThat(listener1InvocationParams.get(1).length, is(1));
        assertThat(listener1InvocationParams.get(1)[0], is("MyEvent2"));
        assertThat(listener1InvocationParams.get(2).length, is(2));
        assertThat(listener1InvocationParams.get(2)[0], is("MyEvent3"));
        assertThat(listener1InvocationParams.get(2)[1], is("MyEvent4"));
    }

    @Test
    public void allObserversForEventInformedWhenEventIsEmitted() {
        int numberOfObservers = 5;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfObservers);
        NoParameterConsumer consumer = () -> countDownLatch.countDown();

        for (int i=0; i<numberOfObservers; i++) {
            eventLoop.observe("Test").subscribe(consumer);
        }
        eventLoop.emit("Test");

        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        assertThat(countDownLatch.getCount(), is(0L));
    }

    @Test
    public void allSubscribersOfAllObserversForEventInformedWhenEventIsEmitted() {
        int numberOfObservers = 5;
        int numberOfSubscriptions = 5;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfObservers * numberOfSubscriptions);
        NoParameterConsumer consumer = () -> countDownLatch.countDown();

        for (int i=0; i<numberOfObservers; i++) {
            Flowable<Object[]> observable = eventLoop.observe("Test");
            for (int j=0; j<numberOfSubscriptions; j++) {
                observable.subscribe(consumer);
            }
        }
        eventLoop.emit("Test");

        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        assertThat(countDownLatch.getCount(), is(0L));
    }

    @Test
    public void testAllRegisteredListenersMatchingEventAreCalledWhenEventIsEmitted() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(3);
        boolean[] invocationFlags = new boolean[3];

        eventLoop.observe(String.class).subscribe(data -> {
            invocationFlags[0] = true;
            countDownLatch.countDown();
        });
        eventLoop.observe(String.class).subscribe(data -> {
            invocationFlags[1] = true;
            countDownLatch.countDown();
        });
        eventLoop.observe(Integer.class).subscribe(data -> {
            invocationFlags[2] = true;
            countDownLatch.countDown();
        });

        eventLoop.emit(String.class, "My String");

        countDownLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount(), is(1L)); // the Integer listener should NOT have been fired
        assertTrue(invocationFlags[0]);
        assertTrue(invocationFlags[1]);
        assertFalse(invocationFlags[2]);
    }

    @Test
    public void testOneOffListenerOnlyCalledOnce() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(3);
        List<String> listenerInvocationParams = new ArrayList<>();
        List<String> oneOffListenerInvocationParams = new ArrayList<>();
        eventLoop.observe(String.class).subscribe(data -> {
            listenerInvocationParams.add((String) data[0]);
            countDownLatch.countDown();
        });
        eventLoop.single(String.class).subscribe(data -> {
            oneOffListenerInvocationParams.add((String) data[0]);
            countDownLatch.countDown();
        });

        eventLoop.emit(String.class, "First");
        eventLoop.emit(String.class, "Second");

        countDownLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount(), is(0L));
        assertThat(listenerInvocationParams.size(), is(2));
        assertThat(oneOffListenerInvocationParams.size(), is(1));
        assertThat(listenerInvocationParams, contains("First", "Second"));
        assertThat(oneOffListenerInvocationParams, contains("First"));
        assertNotNull(eventLoop.subjectFor(String.class));
    }

    @Test
    public void testListenerCanBeRemovedSeparately() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(3);

        List<String> listener1InvocationParams = new ArrayList<>();
        List<String> listener2InvocationParams = new ArrayList<>();
        Disposable d1 = eventLoop.observe(String.class).subscribe(data -> {
            listener1InvocationParams.add((String) data[0]);
            countDownLatch.countDown();
        });
        Disposable d2 = eventLoop.observe(String.class).subscribe(data -> {
                    listener2InvocationParams.add((String) data[0]);
                    countDownLatch.countDown();
                },
                throwable -> {},
                () -> System.out.println("d2 completed"));

        eventLoop.emit(String.class, "MyEvent1");

        d1.dispose();
        eventLoop.emit(String.class, "MyEvent2");

        d2.dispose();
        eventLoop.emit(String.class, "MyEvent3");

        countDownLatch.await(3000,TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount(),is(0L));
        assertThat(listener1InvocationParams.size(), is(1));
        assertThat(listener1InvocationParams, Matchers.contains("MyEvent1"));
        assertThat(listener2InvocationParams.size(), is(2));
        assertThat(listener2InvocationParams, Matchers.contains("MyEvent1", "MyEvent2"));
    }

    @Test
    public void testAllListenersCanBeRemoved() throws Exception {
        boolean[] invocationFlag = new boolean[1];
        Disposable d1 = eventLoop.observe(String.class).subscribe(data -> invocationFlag[0] = true);
        Disposable d2 = eventLoop.single(String.class).subscribe(data -> invocationFlag[0] = true);

        d1.dispose();
        d2.dispose();
        eventLoop.emit(String.class, "MyEvent1");

        Thread.sleep(500);
        assertFalse(invocationFlag[0]);
        assertNull(eventLoop.subjectFor(String.class));
    }

    @Test
    public void allObserversForEventInformedEvenWhenOneThrowsUncaughtException() {
        int numberOfGoodObservers = 5;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfGoodObservers);
        NoParameterConsumer goodConsumer = () -> countDownLatch.countDown();
        NoParameterConsumer badConsumer = () -> { throw new RuntimeException("for test"); };

        for (int i = 0; i < numberOfGoodObservers; i++) {
            eventLoop.observe("Test").subscribe(goodConsumer);
        }
        eventLoop.observe("Test").subscribe(badConsumer);
        eventLoop.emit("Test");

        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        assertThat(countDownLatch.getCount(), is(0L));
    }


    @Test
    public void ensureNumberOfConsumersAreInstantiatedAccordingToSettings() {
        int numberOfThreads = 7;
        int numberOfEvents = 100000;

        EventLoopConfig eventLoopConfig = EventLoopConfig.builder()
                .setDispatchThreadStrategy(EventLoopConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
                .build();
        eventLoop = new EventLoop("test", eventLoopConfig, new Metrics());

        final CountDownLatch latch = new CountDownLatch(1);
        final ConcurrentHashMap<Thread, Thread> threads = new ConcurrentHashMap<>();
        NoParameterConsumer threadDetectingConsumer = () -> threads.put(Thread.currentThread(), Thread.currentThread());
        eventLoop.observe("tick").subscribe(threadDetectingConsumer);
        NoParameterConsumer endConsumer = latch::countDown;
        eventLoop.observe("end").subscribe(endConsumer);

        for (int i = 0; i < numberOfEvents; i++) {
            eventLoop.emit("tick");
        }
        eventLoop.emit("end");

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(numberOfThreads, threads.size());
    }

    @Test
    public void testEventsAreDroppedIfQueueIsFullAndStrategySaysSo() {
        eventLoop = new EventLoop("test", EventLoopConfig.builder()
                .setDispatchThreadStrategy(EventLoopConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
                .setInsufficientCapacityStrategy(EventLoopConfig.InsufficientCapacityStrategy.DROP_EVENTS)
                .build(), new Metrics());
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
        eventLoop.observe("bla").subscribe(consumer);

        for (int i = 0; i < numEvents; i++) {
            eventLoop.emit("bla");
        }

        assertTrue(numEventsProcessed[0] < numEvents);
    }

    @Test
    public void testEventsAreQueuedInMemoryIfQueueIsFullAndStrategySaysSo() {
        eventLoop = new EventLoop("test", EventLoopConfig.builder()
                .setDispatchThreadStrategy(EventLoopConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
                .setInsufficientCapacityStrategy(EventLoopConfig.InsufficientCapacityStrategy.DROP_EVENTS)
                .build(), new Metrics());
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
        eventLoop.observe("bla").subscribe(consumer);

        for (int i = 0; i < numEvents; i++) {
            eventLoop.emit("bla");
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
        eventLoop = new EventLoop("test", EventLoopConfig.builder()
                .setDispatchThreadStrategy(EventLoopConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
                .setInsufficientCapacityStrategy(EventLoopConfig.InsufficientCapacityStrategy.WAIT_UNTIL_SPACE_AVAILABLE)
                .build(), new Metrics());
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
        eventLoop.observe("bla").subscribe(consumer);

        // start producer in a separate thread
        new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < numEvents; i++) {
                    eventLoop.emit("bla");
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
        eventLoop = new EventLoop("test", EventLoopConfig.builder()
                .setDispatchThreadStrategy(EventLoopConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD)
                .build(), new Metrics());
        eventLoop.registerIdProviderForDuplicateEventDetection("bla", data -> "id");
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
        eventLoop.observe("bla").subscribe(consumer);
        eventLoop.single("blo").subscribe(consumer);

        for (int i = 0; i < numEvents; i++) {
            eventLoop.emit("bla", "value" + i);
        }
        eventLoop.emit("blo", "bye");

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(), is(0L));

        assertTrue(numEventsProcessed[0] < numEvents);
        assertThat(lastDataProcessed[0], is("value" + (numEvents - 1)));
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

        Flowable flowable = eventLoop.observe("xxx");

        flowable.subscribe(listener);
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "throw");
        flowable.subscribe(listener);
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "end");

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

        Flowable observable = eventLoop.observe("xxx");

        observable.subscribe(listener);
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "throw");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "end");

        latch.await(2, TimeUnit.SECONDS);

        Assert.assertEquals(2, counter[0]);
    }
}
