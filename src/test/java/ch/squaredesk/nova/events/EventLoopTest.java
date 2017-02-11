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
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.TestObserver;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void uncaughtErrorInSubscriberVoidsObservable() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicInteger i = new AtomicInteger(0);

        Disposable d = eventLoop.on("x")
                .subscribe(
                        data -> {
                            if (i.incrementAndGet()==2)
                            throw new RuntimeException("for test");
                        },
                        t -> countDownLatch.countDown());
        assertFalse(d.isDisposed());

        eventLoop.emit("x");
        eventLoop.emit("x"); // should throw

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(),is(0L));


        assertTrue(d.isDisposed());

        eventLoop.emit("x");
        assertTrue(d.isDisposed());
        assertThat(i.get(), is(2)); // 3rd one should not have been deliverd
    }


    @Test
    public void observableWithUncaughtErrorInObserverIsDestroyed() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Disposable d = eventLoop.on("x")
                .subscribe(
                        data -> {
                            throw new RuntimeException("for test");
                        },
                        t -> countDownLatch.countDown());
        assertFalse(d.isDisposed());

        eventLoop.emit("x"); // throws

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(),is(0L));

        assertTrue(d.isDisposed());
    }

    @Test
    public void eachSubscriptionFedBySameObservable() {
        Object f1 = eventLoop.on("x");
        Object f2 = eventLoop.on("x");
        assertTrue(f1==f2);
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
            Observable<Object[]> observable = eventLoop.on("Test");
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
    public void testListenerCanBeRemovedSeparately() throws Exception {
        TestObserver<Object[]> observer1 = eventLoop.on(String.class).take(1).test();
        TestObserver<Object[]> observer2 = eventLoop.on(String.class).take(2).test();

        // emit -> both observers should get it
        eventLoop.emit(String.class, "MyEvent1");
        assertTrue(observer1.await(100,TimeUnit.MILLISECONDS));
        observer1.dispose();

        // emit again -> observer 2 should also still get this one after unsubscription of observer 1
        eventLoop.emit(String.class, "MyEvent2");
        assertTrue(observer2.await(100,TimeUnit.MILLISECONDS));
        observer2.assertValueCount(2);
    }

    @Test
    public void allListenersCanBeRemoved() throws Exception {
        boolean[] invocationFlag = new boolean[1];
        Disposable d1 = eventLoop.on(String.class).subscribe(data -> invocationFlag[0] = true);
        Disposable d2 = eventLoop.on(String.class).take(1).subscribe(data -> invocationFlag[0] = true);

        d1.dispose();
        d2.dispose();
        eventLoop.emit(String.class, "MyEvent1");

        Thread.sleep(500);
        assertFalse(invocationFlag[0]);
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
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger();

        Consumer<Object[]> listener = param -> {
            if ("throw".equals(param[0])) {
                throw new RuntimeException("for test");
            } else {
                counter.incrementAndGet();
            }
        };

        Disposable d = eventLoop.on("xxx").subscribe(listener, t -> latch.countDown());
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "throw");

        latch.await();
        assertTrue(d.isDisposed());
        assertThat(counter.get(),is(2));

        TestObserver<Object[]> observer = eventLoop.on("xxx").take(3).test();
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "count");
        eventLoop.emit("xxx", "end");
        assertTrue(observer.await(100, TimeUnit.MILLISECONDS));
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

        Observable observable = eventLoop.on("xxx");

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

        Observable observable = eventLoop.on("xxx");

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
