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
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class EventBusTest {
    private EventBus eventBus;

    @BeforeEach
    void setup() {
        eventBus = new EventBus(
                "test",
                new EventBusConfig(BackpressureStrategy.BUFFER, false),
                new Metrics());
    }

    @Test
    void testRegisteringNullEventThrows() {
        Throwable ex = assertThrows(NullPointerException.class,
                () -> eventBus.on(null));
        assertThat(ex.getMessage(), containsString("event"));
    }

    @Test
    void uncaughtErrorInSubscriberVoidsObservable() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicInteger i = new AtomicInteger(0);

        Disposable d = eventBus.on("x")
                .subscribe(
                        data -> {
                            if (i.incrementAndGet()==2)
                            throw new RuntimeException("for test");
                        },
                        t -> countDownLatch.countDown());
        assertFalse(d.isDisposed());

        eventBus.emit("x");
        eventBus.emit("x"); // should throw

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(),is(0L));


        assertTrue(d.isDisposed());

        eventBus.emit("x");
        assertTrue(d.isDisposed());
        assertThat(i.get(), is(2)); // 3rd one should not have been delivered
    }


    @Test
    void observableWithUncaughtErrorInObserverIsDestroyed() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Disposable d = eventBus.on("x")
                .subscribe(
                        data -> {
                            throw new RuntimeException("for test");
                        },
                        t -> countDownLatch.countDown());
        assertFalse(d.isDisposed());

        eventBus.emit("x"); // throws

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(),is(0L));

        assertTrue(d.isDisposed());
    }

    @Test
    void eachSubscriptionFedByDifferentFlowable() {
        Object f1 = eventBus.on("x");
        Object f2 = eventBus.on("x");
        assertTrue(f1!=f2);
    }

    @Test
    void testEmmittingNullThrows() {
        assertThrows(NullPointerException.class, () -> eventBus.emit(null));
    }

    @Test
    void testRegisteredListenerCalledEverytimeAnEventIsEmitted() throws Exception {
        List<Object[]> listener1InvocationParams = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(3);

        eventBus.on(String.class).subscribe(data -> {
            listener1InvocationParams.add(data);
            countDownLatch.countDown();
        });

        eventBus.emit(String.class, "MyEvent1");
        eventBus.emit(String.class, "MyEvent2");
        eventBus.emit(String.class, "MyEvent3", "MyEvent4");

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
    void allObserversForEventInformedWhenEventIsEmitted() throws Exception {
        int numberOfObservers = 5;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfObservers);
        NoParameterConsumer consumer = countDownLatch::countDown;

        for (int i=0; i<numberOfObservers; i++) {
            eventBus.on("Test").subscribe(consumer);
        }
        eventBus.emit("Test");

        countDownLatch.await(1, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(), is(0L));
    }

    @Test
    void allSubscribersOfAllObserversForEventInformedWhenEventIsEmitted() throws Exception {
        int numberOfObservers = 5;
        int numberOfSubscriptions = 5;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfObservers * numberOfSubscriptions);
        NoParameterConsumer consumer = countDownLatch::countDown;

        for (int i=0; i<numberOfObservers; i++) {
            Flowable<Object[]> observable = eventBus.on("Test");
            for (int j=0; j<numberOfSubscriptions; j++) {
                observable.subscribe(consumer);
            }
        }
        eventBus.emit("Test");

        countDownLatch.await(1, TimeUnit.SECONDS);

        assertThat(countDownLatch.getCount(), is(0L));
    }

    @Test
    void testAllRegisteredListenersMatchingEventAreCalledWhenEventIsEmitted() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(3);
        boolean[] invocationFlags = new boolean[3];

        eventBus.on(String.class).subscribe(data -> {
            invocationFlags[0] = true;
            countDownLatch.countDown();
        });
        eventBus.on(String.class).subscribe(data -> {
            invocationFlags[1] = true;
            countDownLatch.countDown();
        });
        eventBus.on(Integer.class).subscribe(data -> {
            invocationFlags[2] = true;
            countDownLatch.countDown();
        });

        eventBus.emit(String.class, "My String");

        countDownLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount(), is(1L)); // the Integer listener should NOT have been fired
        assertTrue(invocationFlags[0]);
        assertTrue(invocationFlags[1]);
        assertFalse(invocationFlags[2]);
    }

    @Test
    void testListenerCanBeRemovedSeparately() throws Exception {
        TestSubscriber<Object[]> observer1 = eventBus.on(String.class).take(1).test();
        TestSubscriber<Object[]> observer2 = eventBus.on(String.class).take(2).test();

        // emit -> both observers should get it
        eventBus.emit(String.class, "MyEvent1");
        assertTrue(observer1.await(100,TimeUnit.MILLISECONDS));
        observer1.dispose();

        // emit again -> observer 2 should also still get this one after unsubscription of observer 1
        eventBus.emit(String.class, "MyEvent2");
        assertTrue(observer2.await(100,TimeUnit.MILLISECONDS));
        observer2.assertValueCount(2);
    }

    @Test
    void allListenersCanBeRemoved() throws Exception {
        boolean[] invocationFlag = new boolean[1];
        Disposable d1 = eventBus.on(String.class).subscribe(data -> invocationFlag[0] = true);
        Disposable d2 = eventBus.on(String.class).take(1).subscribe(data -> invocationFlag[0] = true);

        d1.dispose();
        d2.dispose();
        eventBus.emit(String.class, "MyEvent1");

        Thread.sleep(500);
        assertFalse(invocationFlag[0]);
    }

    @Test
    void allObserversForEventInformedEvenWhenOneThrowsUncaughtException() {
        int numberOfGoodObservers = 5;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfGoodObservers);
        NoParameterConsumer goodConsumer = countDownLatch::countDown;
        NoParameterConsumer badConsumer = () -> { throw new RuntimeException("for test"); };

        for (int i = 0; i < numberOfGoodObservers; i++) {
            eventBus.on("Test").subscribe(goodConsumer);
        }
        eventBus.on("Test").subscribe(badConsumer);
        eventBus.emit("Test");

        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        assertThat(countDownLatch.getCount(), is(0L));
    }

    @Test
    void subscriptionDiesOnUnhandledObserverException() throws InterruptedException {
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

        Flowable observable = eventBus.on("xxx");

        observable.subscribe(listener);
        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "throw");
        eventBus.emit("xxx", "count"); // no count, since observer dead
        eventBus.emit("xxx", "count"); // no count, since observer dead
        eventBus.emit("xxx", "end");

        latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(latch.getCount(),is(1L)); // no count down, since "end" was emitted after throw
        assertThat(counter.get(), is(2));
    }

    @Test
    void otherSubscriptionAreNotAffectedIfObserverThrowsUnhandledException() throws InterruptedException {
        class NastyConsumer implements Consumer<Object[]> {
            private int counter = 0;
            CountDownLatch latch = new CountDownLatch(1);
            @Override
            public void accept(Object[] param) throws Exception {
                if ("throw".equals(param[0])) {
                    throw new RuntimeException("for test");
                } else if ("end".equals(param[0])) {
                    latch.countDown();
                } else {
                    counter++;
                }
            }
        }
        class NiceConsumer implements Consumer<Object[]> {
            private int counter = 0;
            CountDownLatch latch = new CountDownLatch(1);
            @Override
            public void accept(Object[] param) throws Exception {
                if ("end".equals(param[0])) {
                    latch.countDown();
                } else {
                    counter++;
                }
            }
        }
        NastyConsumer consumer1 = new NastyConsumer();
        NiceConsumer consumer2 = new NiceConsumer();

        Disposable d1 = eventBus.on("xxx").subscribe(consumer1);
        Disposable d2 = eventBus.on("xxx").subscribe(consumer2);

        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "throw");
        eventBus.emit("xxx", "count"); // no count on consumer1, since observer dead
        eventBus.emit("xxx", "count"); // no count on consumer1, since observer dead
        eventBus.emit("xxx", "end");

        consumer2.latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(consumer1.latch.getCount(),is(1L)); // no count down, since "end" was emitted after throw
        assertThat(consumer2.latch.getCount(),is(0L));
        assertThat(consumer1.counter, is(2));
        assertThat(consumer2.counter, is(5)); // 4*count + 1*throw
    }

    @Test
    void subscriptionSurvivesOnUnhandledObserverExceptionIfNovaConsumerUsed() throws InterruptedException {
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

        Flowable observable = eventBus.on("xxx");

        observable.subscribe(listener);
        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "throw");
        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "end");

        latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(latch.getCount(),is(0L));
        assertThat(counter.get(),is(4));
    }
}
