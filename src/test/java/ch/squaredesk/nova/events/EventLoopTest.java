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
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.Subject;
import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class EventLoopTest {
    private EventLoop eventLoop;

    @Before
    public void setup() {
        eventLoop = new EventLoop(
                "test",
                EventLoopConfig.builder().setDispatchInEmitterThread(false).build(),
                new Metrics());
    }

    @BeforeClass
    public static void initLogging() {
        BasicConfigurator.configure();
    }

    @Test(expected = NullPointerException.class)
    public void testRegisteringNullEventThrows() {
        eventLoop.on(null);
    }


    @Test
    public void unsubscribingLastObserverUnregistersListener() {
        Disposable d = eventLoop.on("x").subscribe(data -> { });
        assertNotNull(eventLoop.subjectFor("x"));
        d.dispose();
        assertNull(eventLoop.subjectFor("x"));
    }

    @Test
    public void uncaughtErrorInSubscriberVoidsObservable() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicInteger i = new AtomicInteger(0);

        Disposable d = eventLoop.on("x")
                .doFinally(() -> countDownLatch.countDown())
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

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(),is(0L));


        assertTrue(d.isDisposed());
        assertNull(eventLoop.subjectFor("x"));

        eventLoop.emit("x");
        assertTrue(d.isDisposed());
        assertThat(i.get(), is(2)); // 3rd one should not have been deliverd
    }


    @Test
    public void observableWithUncaughtErrorInObserverIsDestroyed() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Disposable d = eventLoop.on("x")
                .doFinally(() -> countDownLatch.countDown())
                .subscribe(
                        data -> {
                            throw new RuntimeException("for test");
                        },
                        t -> {});
        assertNotNull(eventLoop.subjectFor("x"));
        assertFalse(d.isDisposed());

        eventLoop.emit("x"); // throws

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(),is(0L));

        assertTrue(d.isDisposed());
        assertNull(eventLoop.subjectFor("x"));
    }

    @Test
    public void subjectIsEagerlyRegistered() {
        assertNull(eventLoop.subjectFor("x"));
        eventLoop.on("x");
        assertNotNull(eventLoop.subjectFor("x"));
    }

    @Test
    public void eachSubscriptionFedBySameSubject() {
        assertNull(eventLoop.subjectFor("x"));
        Flowable f = eventLoop.on("x");
        assertNotNull(eventLoop.subjectFor("x"));
        f.subscribe(data -> {});
        Subject<Object[]> consumer = eventLoop.subjectFor("x");
        assertNotNull(consumer);
        f.subscribe(data -> {});
        Subject<Object[]> consumer2 = eventLoop.subjectFor("x");
        assertTrue(consumer==consumer2);
    }

    @Test(expected = NullPointerException.class)
    public void testEmmittingNullThrows() {
        eventLoop.emit(null);
    }

    @Test
    public void testRegisteredListenerCalledEverytimeAnEventIsEmitted() throws Exception {
        List<Object[]> listener1InvocationParams = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(3);

        eventLoop.on(String.class).subscribe(data -> {
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
            eventLoop.on("Test").subscribe(consumer);
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
            Flowable<Object[]> observable = eventLoop.on("Test");
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

        eventLoop.on(String.class).subscribe(data -> {
            invocationFlags[0] = true;
            countDownLatch.countDown();
        });
        eventLoop.on(String.class).subscribe(data -> {
            invocationFlags[1] = true;
            countDownLatch.countDown();
        });
        eventLoop.on(Integer.class).subscribe(data -> {
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
    @Ignore("Listeners don't get regsitered. WTF????") // FIXME fix test
    public void testListenerCanBeRemovedSeparately() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(3);

        List<String> listener1InvocationParams = new ArrayList<>();
        List<String> listener2InvocationParams = new ArrayList<>();
        Disposable d1 = eventLoop.on(String.class).subscribe(data -> {
            System.out.println("1111111" + data);
            listener1InvocationParams.add((String) data[0]);
            countDownLatch.countDown();
        });
        Disposable d2 = eventLoop.on(String.class).subscribe(data -> {
                    System.out.println("22222" + data);
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

        countDownLatch.await(1000,TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount(),is(0L));
        assertThat(listener1InvocationParams.size(), is(1));
        assertThat(listener1InvocationParams, Matchers.contains("MyEvent1"));
        assertThat(listener2InvocationParams.size(), is(2));
        assertThat(listener2InvocationParams, Matchers.contains("MyEvent1", "MyEvent2"));
    }

    @Test
    public void allListenersCanBeRemoved() throws Exception {
        boolean[] invocationFlag = new boolean[1];
        Disposable d1 = eventLoop.on(String.class).subscribe(data -> invocationFlag[0] = true);
        Disposable d2 = eventLoop.on(String.class).take(1).subscribe(data -> invocationFlag[0] = true);

        assertNotNull(eventLoop.subjectFor(String.class));

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
            eventLoop.on("Test").subscribe(goodConsumer);
        }
        eventLoop.on("Test").subscribe(badConsumer);
        eventLoop.emit("Test");

        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        assertThat(countDownLatch.getCount(), is(0L));
    }




    @Test
    public void eventLoopDoesntDieOnUnhandledException() throws InterruptedException {
        final int[] counter = new int[1];
        final CountDownLatch latch = new CountDownLatch(1);

        Consumer<Object[]> listener = param -> {
            if ("throw".equals(param[0])) {
                throw new RuntimeException("for test");
            } else if ("end".equals(param[0])) {
                latch.countDown();
            } else {
                counter[0]++;
            }
        };

        eventLoop.on("xxx").subscribe(listener);
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "throw");
        eventLoop.on("xxx").subscribe(listener);
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "end");

        latch.await(2, TimeUnit.SECONDS);

        Assert.assertEquals(4, counter[0]);
    }

    @Test
    public void subscriptionDiesOnUnhandledObserverException() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        Consumer<Object[]> listener = param -> {
            if ("throw".equals(param[0])) {
                throw new RuntimeException("for test");
            } else if ("end".equals(param[0])) {
                latch.countDown();
            } else {
                counter.incrementAndGet();
            }
        };

        Flowable observable = eventLoop.on("xxx");

        observable.subscribe(listener);
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "throw");
        eventLoop.emit("xxx", "count"); // no count, since observer dead
        eventLoop.emit("xxx", "count"); // no count, since observer dead
        eventLoop.emit("xxx", "end");

        latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(latch.getCount(),is(1L)); // no count down, since "end" was emitted after throw
        assertThat(counter.get(), is(2));
    }

    @Test
    public void subscriptionSurvivesOnUnhandledObserverExceptionIfNovaConsumerUsed() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);

        SingleParameterConsumer<String> listener = param -> {
            if ("throw".equals(param)) {
                throw new RuntimeException("for test");
            } else if ("end".equals(param)) {
                latch.countDown();
            } else {
                counter.incrementAndGet();
            }
        };

        Flowable observable = eventLoop.on("xxx");

        observable.subscribe(listener);
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "throw");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "end");

        latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(latch.getCount(),is(0L));
        assertThat(counter.get(),is(4));
    }
}
