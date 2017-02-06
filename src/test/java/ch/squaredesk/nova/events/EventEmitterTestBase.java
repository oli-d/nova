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
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;
import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public abstract class EventEmitterTestBase {
    private EventEmitter eventEmitter;

    @Before
    public void setup() {
        eventEmitter = createEventEmitter();
    }

    protected abstract EventEmitter createEventEmitter();

    protected abstract EventEmitter createEventEmitter(EventDispatchConfig eventDispatchConfig);

    @BeforeClass
    public static void initLogging() {
        BasicConfigurator.configure();
    }

    @Test(expected = NullPointerException.class)
    public void testRegisteringNullEventThrows() {
        eventEmitter.observe(null);
    }


    @Test
    public void unsubscribingLastObserverUnregistersListener() {
        Disposable d = eventEmitter.observe("x").subscribe(data -> { });
        assertNotNull(eventEmitter.consumerFor("x"));
        d.dispose();
        assertNull(eventEmitter.consumerFor("x"));
    }

    @Test
    public void uncaughtErrorInSubscriberVoidsObservable() {
        AtomicInteger i = new AtomicInteger(0);

        Disposable d = eventEmitter.observe("x")
                .subscribe(
                        data -> {
                            if (i.incrementAndGet()==2)
                            throw new RuntimeException("for test");
                        },
                        t -> {});
        assertNotNull(eventEmitter.consumerFor("x"));
        assertFalse(d.isDisposed());

        eventEmitter.emit("x");
        eventEmitter.emit("x"); // should throw
        assertTrue(d.isDisposed());
        assertNotNull(eventEmitter.consumerFor("x"));

        eventEmitter.emit("x");
        assertTrue(d.isDisposed());
        assertThat(i.get(), is(2)); // 3rd one should not have been deliverd
    }


    public static void main2(String[] args) {
//        PublishSubject<String> ps = PublishSubject.create();
        Observable<String> ps = Observable.create(s -> {
            System.out.println("Here we are!!!");
            s.setDisposable(new Disposable() {
                @Override
                public void dispose() {
                    System.out.println("Last one gone");
                }

                @Override
                public boolean isDisposed() {
                    return false;
                }
            });
            s.onNext("1");
            s.onNext("2");
            s.onNext("3");
            s.onNext("4");
            s.onNext("5");
            s.onNext("6");
            s.onNext("7");
            s.onNext("8");
            s.onComplete();
        });
        ConnectableObservable<String> cs = ps.cache().publish();
        Observable<String> refCount = cs.refCount();

//        ps.doOnNext(s -> System.out.println("PS: publish " + s));
//        ps.doOnError(t -> System.out.println("PS: got error " + t));
//        ps.doOnTerminate(() -> System.out.println("PS: on terminate() "));
//        ps.doOnSubscribe(d -> System.out.println("PS: subscribed new Disposable "));
//        ps.doOnDispose(() -> System.out.println("PS: disposed Disposable "));
//        cs.doOnNext(s -> System.out.println("CS: publish " + s));
//        cs.doOnError(t -> System.out.println("CS: got error " + t));
//        cs.doOnTerminate(() -> System.out.println("CS: on terminate() "));
//        cs.doOnSubscribe(d -> System.out.println("CS: subscribed new Disposable "));
//        cs.doOnDispose(() -> System.out.println("CS: disposed Disposable "));
//        refCount = refCount.doOnNext(s -> System.out.println("RC: publish " + s));
//        refCount = refCount.doOnError(t -> System.out.println("RC: got error " + t));
//        refCount = refCount.doOnTerminate(() -> System.out.println("RC: on terminate() "));
//        refCount = refCount.doOnSubscribe(d -> System.out.println("RC: subscribed new Disposable "));
//        refCount = refCount.doOnDispose(() -> System.out.println("RC: disposed Disposable "));

        AtomicInteger i = new AtomicInteger(0);
        Disposable d1 = refCount.map(s -> {
            System.out.println("C1 got" + s);
            return s;
        }).subscribe();
        Disposable d2 = refCount.map(s -> {
            System.out.println("C2 got " + s);
            /*if (i.incrementAndGet() % 3 == 0) throw new RuntimeException("x");
            else*/ return s;
        }).subscribe();

        d1.dispose();

        d2.dispose();
        d1 = refCount.map(s -> { System.out.println("C3 got " + s); return s; }).subscribe();
        d1.dispose();
    }
    @Test
    public void observableWithUncaughtErrorCannotProperlyBeDisposed() {
        Disposable d = eventEmitter.observe("x")
                .subscribe(
                        data -> {
                            throw new RuntimeException("for test");
                        },
                        t -> {});
        assertNotNull(eventEmitter.consumerFor("x"));
        assertFalse(d.isDisposed());

        eventEmitter.emit("x"); // throws

        assertTrue(d.isDisposed());
        assertNotNull(eventEmitter.consumerFor("x"));

        d.dispose();
        assertTrue(d.isDisposed());
        assertNotNull(eventEmitter.consumerFor("x"));
    }

    @Test
    public void listenerIsLazilyRegistered() {
        Observable o = eventEmitter.observe("x");
        assertNull(eventEmitter.consumerFor("x"));
        o.subscribe(data -> {});
        assertNotNull(eventEmitter.consumerFor("x"));
    }

    @Test
    public void eachSubscriptionTriggeredBySameListener() {
        Observable o = eventEmitter.observe("x");
        assertNull(eventEmitter.consumerFor("x"));
        o.subscribe(data -> {});
        Consumer<Object[]> consumer = eventEmitter.consumerFor("x");
        assertNotNull(consumer);
        o.subscribe(data -> {});
        Consumer<Object[]> consumer2 = eventEmitter.consumerFor("x");
        assertTrue(consumer==consumer2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmmittingNullThrows() {
        eventEmitter.emit(null);
    }

    @Test
    public void testRegisteredListenerCalledEverytimeAnEventIsEmitted() throws Exception {
        List<Object[]> listener1InvocationParams = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(3);

        eventEmitter.observe(String.class).subscribe(data -> {
            listener1InvocationParams.add(data);
            countDownLatch.countDown();
        });

        eventEmitter.emit(String.class, "MyEvent1");
        eventEmitter.emit(String.class, "MyEvent2");
        eventEmitter.emit(String.class, "MyEvent3", "MyEvent4");

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
            eventEmitter.observe("Test").subscribe(consumer);
        }
        eventEmitter.emit("Test");

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
            Observable<Object[]> observable = eventEmitter.observe("Test");
            for (int j=0; j<numberOfSubscriptions; j++) {
                observable.subscribe(consumer);
            }
        }
        eventEmitter.emit("Test");

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

        eventEmitter.observe(String.class).subscribe(data -> {
            invocationFlags[0] = true;
            countDownLatch.countDown();
        });
        eventEmitter.observe(String.class).subscribe(data -> {
            invocationFlags[1] = true;
            countDownLatch.countDown();
        });
        eventEmitter.observe(Integer.class).subscribe(data -> {
            invocationFlags[2] = true;
            countDownLatch.countDown();
        });

        eventEmitter.emit(String.class, "My String");

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
        eventEmitter.observe(String.class).subscribe(data -> {
            listenerInvocationParams.add((String) data[0]);
            countDownLatch.countDown();
        });
        eventEmitter.single(String.class).subscribe(data -> {
            oneOffListenerInvocationParams.add((String) data[0]);
            countDownLatch.countDown();
        });

        eventEmitter.emit(String.class, "First");
        eventEmitter.emit(String.class, "Second");

        countDownLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount(), is(0L));
        assertThat(listenerInvocationParams.size(), is(2));
        assertThat(oneOffListenerInvocationParams.size(), is(1));
        assertThat(listenerInvocationParams, contains("First", "Second"));
        assertThat(oneOffListenerInvocationParams, contains("First"));
        assertNotNull(eventEmitter.consumerFor(String.class));
    }

    @Test
    public void testListenerCanBeRemovedSeparately() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(3);

        List<String> listener1InvocationParams = new ArrayList<>();
        List<String> listener2InvocationParams = new ArrayList<>();
        Disposable d1 = eventEmitter.observe(String.class).subscribe(data -> {
            listener1InvocationParams.add((String) data[0]);
            countDownLatch.countDown();
        });
        Disposable d2 = eventEmitter.observe(String.class).subscribe(data -> {
                    listener2InvocationParams.add((String) data[0]);
                    countDownLatch.countDown();
                },
                throwable -> {},
                () -> System.out.println("d2 completed"));

        eventEmitter.emit(String.class, "MyEvent1");

        d1.dispose();
        eventEmitter.emit(String.class, "MyEvent2");

        d2.dispose();
        eventEmitter.emit(String.class, "MyEvent3");

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
        Disposable d1 = eventEmitter.observe(String.class).subscribe(data -> invocationFlag[0] = true);
        Disposable d2 = eventEmitter.single(String.class).subscribe(data -> invocationFlag[0] = true);

        d1.dispose();
        d2.dispose();
        eventEmitter.emit(String.class, "MyEvent1");

        if (eventEmitter instanceof EventLoopAwareEventEmitter) {
            Thread.sleep(500);
        }
        assertFalse(invocationFlag[0]);
        assertNull(eventEmitter.consumerFor(String.class));
    }

    @Test
    public void allObserversForEventInformedEvenWhenOneThrowsUncaughtException() {
        int numberOfGoodObservers = 5;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfGoodObservers);
        NoParameterConsumer goodConsumer = () -> countDownLatch.countDown();
        NoParameterConsumer badConsumer = () -> { throw new RuntimeException("for test"); };

        for (int i = 0; i < numberOfGoodObservers; i++) {
            eventEmitter.observe("Test").subscribe(goodConsumer);
        }
        eventEmitter.observe("Test").subscribe(badConsumer);
        eventEmitter.emit("Test");

        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        assertThat(countDownLatch.getCount(), is(0L));
    }


}
