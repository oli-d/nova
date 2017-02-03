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

import org.apache.log4j.BasicConfigurator;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    @BeforeClass
    public static void initLogging() {
        BasicConfigurator.configure();
    }

    @Test(expected = NullPointerException.class)
    public void testRegisteringNullEventThrows() {
        eventEmitter.observe(null);
    }

//    @Test(expected = NullPointerException.class)
//    public void testRemovingAllWithNullEventThrows() {
//        eventEmitter.removeAllListeners(null);
//    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsubscribingObserverUnregistersListener() {
        Assert.fail();
//        eventEmitter.removeListener(String.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmmittingNullThrows() {
        eventEmitter.emit(null);
    }

    /*
    @Test
    public void testGetAllListeners() {
        EventListener listener1 = data -> {
        };
        EventListener listener2 = data -> {
        };

        eventEmitter.on(String.class, listener1);
        eventEmitter.once(String.class, listener2);

        assertTrue(eventEmitter.getListeners("String.class").isEmpty());
        assertEquals(2, eventEmitter.getListeners(String.class).size());
        assertTrue(eventEmitter.getListeners(String.class).contains(listener1));
        assertTrue(eventEmitter.getListeners(String.class).contains(listener2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllListenersWithNullEventThrows() {
        eventEmitter.getListeners(null);
    }

    @Test
    public void testListenerCanBeRemovedSeparately() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(3);

        List<String> listener1InvocationParams = new ArrayList<>();
        List<String> listener2InvocationParams = new ArrayList<>();
        EventListener listener1 = data -> {
            listener1InvocationParams.add((String) data[0]);
            countDownLatch.countDown();
        };
        EventListener listener2 = data -> {
            listener2InvocationParams.add((String) data[0]);
            countDownLatch.countDown();
        };

        eventEmitter.on(String.class, listener1);
        eventEmitter.on(String.class, listener2);

        eventEmitter.emit(String.class, "MyEvent1");

        eventEmitter.removeListener(String.class, listener1);
        eventEmitter.emit(String.class, "MyEvent2");

        eventEmitter.removeListener(String.class, listener2);
        eventEmitter.emit(String.class, "MyEvent3");

        countDownLatch.await(500,TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount(),is(0L));
        assertThat(listener1InvocationParams.size(), is(1));
        assertThat(listener1InvocationParams, Matchers.contains("MyEvent1"));
        assertThat(listener2InvocationParams.size(), is(2));
        assertThat(listener2InvocationParams, Matchers.contains("MyEvent1", "MyEvent2"));
    }

    @Test
    public void testAllListenersCanBeRemoved() throws Exception {
        boolean[] invocationFlag = new boolean[1];
        EventListener listener1 = data -> invocationFlag[0] = true;
        EventListener listener2 = data -> invocationFlag[0] = true;

        eventEmitter.on(String.class, listener1);
        eventEmitter.once(String.class, listener2);

        eventEmitter.removeAllListeners(String.class);
        eventEmitter.emit(String.class, "MyEvent1");

        if (eventEmitter instanceof EventLoopAwareEventEmitter) {
            Thread.sleep(500);
        }
        assertFalse(invocationFlag[0]);
        assertTrue(eventEmitter.getListeners(String.class).isEmpty());
    }

    @Test
    public void testListenersAreRemovedFromNormalAndOneOffMaps() throws Exception  {
        boolean[] invocationFlag = new boolean[1];
        EventListener listener1 = data -> invocationFlag[0] = true;

        eventEmitter.on(String.class, listener1);
        eventEmitter.once(String.class, listener1);

        eventEmitter.removeListener(String.class, listener1);
        eventEmitter.emit("MyEvent1");

        if (eventEmitter instanceof EventLoopAwareEventEmitter) {
            Thread.sleep(500);
        }
        assertFalse(invocationFlag[0]);
        assertTrue(eventEmitter.getListeners(String.class).isEmpty());
    }

    @Test
    public void testRegisteredListenerIsCalledWithoutDataBeingPassed() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        boolean[] invocationFlag = new boolean[1];
        EventListener listener1 = data -> {
            invocationFlag[0] = true;
            countDownLatch.countDown();
        };

        eventEmitter.on(String.class, listener1);
        eventEmitter.emit(String.class);


        countDownLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount(), is(0L));
        assertTrue(invocationFlag[0]);
        assertFalse(eventEmitter.getListeners(String.class).isEmpty());
    }

    @Test
    public void testRegisteredListenerCalledEverytimeAnEventIsEmitted() throws Exception {
        List<Object[]> listener1InvocationParams = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(3);
        EventListener listener1 = data -> {
            listener1InvocationParams.add(data);
            countDownLatch.countDown();
        };


        eventEmitter.on(String.class, listener1);

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
    public void testAllRegisteredListenersMatchingEventAreCalledWhenEventIsEmitted() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(3);
        boolean[] invocationFlags = new boolean[3];
        EventListener listener1 = data -> {
            invocationFlags[0] = true;
            countDownLatch.countDown();
        };
        EventListener listener2 = data -> {
            invocationFlags[1] = true;
            countDownLatch.countDown();
        };
        EventListener listener3 = data -> {
            invocationFlags[2] = true;
            countDownLatch.countDown();
        };

        eventEmitter.on(String.class, listener1);
        eventEmitter.on(String.class, listener2);
        eventEmitter.on(Integer.class, listener3);

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
        EventListener listener = data -> {
            listenerInvocationParams.add((String) data[0]);
            countDownLatch.countDown();
        };
        EventListener oneOffListener = data -> {
            oneOffListenerInvocationParams.add((String) data[0]);
            countDownLatch.countDown();
        };

        eventEmitter.on(String.class, listener);
        eventEmitter.once(String.class, oneOffListener);

        eventEmitter.emit(String.class, "First");
        eventEmitter.emit(String.class, "Second");

        countDownLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount(), is(0L));
        assertThat(listenerInvocationParams.size(), is(2));
        assertThat(oneOffListenerInvocationParams.size(), is(1));
        assertThat(listenerInvocationParams, contains("First", "Second"));
        assertThat(oneOffListenerInvocationParams, contains("First"));
        assertThat(eventEmitter.getListeners(String.class).size(), is(1));
    }

    @Test
    public void testAllListenerCalledDuringDispatchingAlthoughOneMightThrow() throws Exception {
        int[] invocationCounts = new int[3];
        boolean[] throwFlag = new boolean[1];
        CountDownLatch countDownLatch = new CountDownLatch(6);

        eventEmitter.on(String.class, data -> {
            invocationCounts[0]++;
            countDownLatch.countDown();
        });
        eventEmitter.on(String.class, data -> {
            invocationCounts[1]++;
            try {
                if ("Second".equals(data[0])) {
                    throwFlag[0] = true;
                    throw new RuntimeException("For test");
                }
            } finally {
                countDownLatch.countDown();
            }
        });
        eventEmitter.on(String.class, data -> {
            invocationCounts[2]++;
            countDownLatch.countDown();
        });

        eventEmitter.emit(String.class, "First");
        eventEmitter.emit(String.class, "Second");

        countDownLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount(), is(0L));
        assertThat(invocationCounts[0], is(2));
        assertThat(invocationCounts[1], is(2));
        assertThat(invocationCounts[2], is(2));
        assertThat(throwFlag[0], is(true));
    }
*/
}
