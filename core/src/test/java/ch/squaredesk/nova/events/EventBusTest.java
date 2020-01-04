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
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class EventBusTest {
    private EventBus eventBus;

    @BeforeEach
    void setup() {
        eventBus = new EventBus(
                "test",
                new EventDispatchConfig(BackpressureStrategy.BUFFER, false, false, 1),
                new Metrics());
    }

    @Test
    void testRegisteringNullEventThrows() {
        Throwable ex = assertThrows(NullPointerException.class,
                () -> eventBus.on(null));
        assertThat(ex.getMessage(), containsString("event"));
    }

    @Test
    void uncaughtErrorInSubscriberVoidsObservable() {
        AtomicInteger i = new AtomicInteger(0);
        Disposable d = eventBus.on("x")
                .subscribe(
                        data -> {
                            if (i.incrementAndGet()==2) throw new RuntimeException("for test");
                        });

        eventBus.emit("x");
        assertThat(i.get(), is(1));
        assertFalse(d.isDisposed());

        eventBus.emit("x"); // should throw
        assertThat(i.get(), is(2));
        assertTrue(d.isDisposed());

        eventBus.emit("x");
        assertTrue(d.isDisposed());
        assertThat(i.get(), is(2)); // 3rd one should not have been delivered
    }


    @Test
    void eachSubscriptionFedByDifferentFlowable() {
        Object f1 = eventBus.on("x");
        Object f2 = eventBus.on("x");
        assertThat(f1, not(sameInstance(f2)));
    }

    @Test
    void testEmmittingNullThrows() {
        assertThrows(NullPointerException.class, () -> eventBus.emit(null));
    }

    @Test
    void testRegisteredListenerCalledEverytimeAnEventIsEmitted() {
        List<Object[]> listener1InvocationParams = new ArrayList<>();
        eventBus.on(String.class).subscribe(data -> {
            listener1InvocationParams.add(data);
        });

        eventBus.emit(String.class, "MyEvent1");
        eventBus.emit(String.class, "MyEvent2");
        eventBus.emit(String.class, "MyEvent3", "MyEvent4");

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
    void allObserversForEventInformedWhenEventIsEmitted() {
        int numberOfObservers = 5;
        AtomicInteger counter = new AtomicInteger();
        NoParameterConsumer consumer = counter::incrementAndGet;

        for (int i=0; i<numberOfObservers; i++) {
            eventBus.on("Test").subscribe(consumer);
        }
        eventBus.emit("Test");

        assertThat(counter.get(), is(numberOfObservers));
    }

    @Test
    void allSubscribersOfAllObservablesForEventInformedWhenEventIsEmitted() {
        int numberOfObservables = 5;
        int numberOfSubscriptions = 5;
        AtomicInteger counter = new AtomicInteger();
        NoParameterConsumer consumer = counter::incrementAndGet;

        for (int i=0; i<numberOfObservables; i++) {
            Flowable<Object[]> observable = eventBus.on("Test");
            for (int j=0; j<numberOfSubscriptions; j++) {
                observable.subscribe(consumer);
            }
        }
        eventBus.emit("Test");

        assertThat(counter.get(), is(numberOfObservables * numberOfSubscriptions));
    }

    @Test
    void testAllRegisteredListenersMatchingEventAreCalledWhenEventIsEmitted() {
        TestSubscriber<Object[]> observer1 = eventBus.on(String.class).test();
        TestSubscriber<Object[]> observer2 = eventBus.on(String.class).test();
        TestSubscriber<Object[]> observer3 = eventBus.on(Integer.class).test();

        eventBus.emit(String.class, "My String");

        observer1.assertValueCount(1);
        observer2.assertValueCount(1);
        observer3.assertEmpty();
    }

    @Test
    void testListenerCanBeRemovedSeparately() {
        TestSubscriber<Object[]> observer1 = eventBus.on(String.class).test();
        TestSubscriber<Object[]> observer2 = eventBus.on(String.class).test();

        // emit -> both observers should get it
        eventBus.emit(String.class, "MyEvent1");
        observer1.dispose();
        // emit again -> observer 2 should still get this after observer 1 was disposed
        eventBus.emit(String.class, "MyEvent2");

        observer1.assertValueCount(1);
        observer2.assertValueCount(2);
    }

    @Test
    void allListenersCanBeRemoved() {
        boolean[] invocationFlag = new boolean[1];
        Disposable d1 = eventBus.on(String.class).subscribe(data -> invocationFlag[0] = true);
        Disposable d2 = eventBus.on(String.class).subscribe(data -> invocationFlag[0] = true);

        d1.dispose();
        d2.dispose();
        eventBus.emit(String.class, "MyEvent1");

        assertFalse(invocationFlag[0]);
    }

    @Test
    void allObserversForEventInformedEvenWhenOneThrowsUncaughtException() {
        int numberOfGoodObservers = 5;
        AtomicInteger counter = new AtomicInteger();
        NoParameterConsumer goodConsumer = counter::incrementAndGet;
        NoParameterConsumer badConsumer = () -> { throw new RuntimeException("for test"); };
        eventBus.on("Test").subscribe(badConsumer);
        for (int i = 0; i < numberOfGoodObservers; i++) {
            eventBus.on("Test").subscribe(goodConsumer);
        }

        eventBus.emit("Test");

        assertThat(counter.get(), is(numberOfGoodObservers));
    }

    @Test
    void subscriptionDiesOnUnhandledObserverException() {
        NastyConsumer nastyConsumer = new NastyConsumer();
        eventBus.on("xxx").subscribe(nastyConsumer);

        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "throw");
        eventBus.emit("xxx", "count"); // no count, since observer dead
        eventBus.emit("xxx", "count"); // no count, since observer dead
        eventBus.emit("xxx", "end");

        assertFalse(nastyConsumer.completed); // not set since "end" was emitted after throw
        assertThat(nastyConsumer.counter, is(2));
    }

    @Test
    void otherSubscriptionAreNotAffectedIfObserverThrowsUnhandledException() {
        NastyConsumer consumer1 = new NastyConsumer();
        NiceConsumer consumer2 = new NiceConsumer();
        eventBus.on("xxx").subscribe(consumer1);
        eventBus.on("xxx").subscribe(consumer2);

        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "count");
        eventBus.emit("xxx", "throw");
        eventBus.emit("xxx", "count"); // no count on consumer1, since observer dead
        eventBus.emit("xxx", "count"); // no count on consumer1, since observer dead
        eventBus.emit("xxx", "end");

        assertFalse(consumer1.completed);
        assertTrue(consumer2.completed);
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
        eventBus.on("xxx").subscribe(listener);

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

    private class NastyConsumer implements Consumer<Object[]> {
        private int counter = 0;
        private boolean completed;

        @Override
        public void accept(Object[] param) {
            if ("throw".equals(param[0])) {
                throw new RuntimeException("for test");
            } else if ("end".equals(param[0])) {
                completed = true;
            } else {
                counter++;
            }
        }
    }

    class NiceConsumer implements Consumer<Object[]> {
        private int counter = 0;
        private boolean completed;

        @Override
        public void accept(Object[] param) {
            if ("end".equals(param[0])) {
                completed = true;
            } else {
                counter++;
            }
        }
    }
}
