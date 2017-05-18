/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events.consumers;

import ch.squaredesk.nova.events.EventBus;
import ch.squaredesk.nova.events.EventBusConfig;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.functions.Consumer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class ConsumerWrappersTest {
    @Test
    void testInvokingWithoutParams() throws Exception {
        final CountDownLatch[] latches = new CountDownLatch[12];
        for (int i = 0; i < latches.length; i++) {
            latches[i] = new CountDownLatch(1);
        }

        TenParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean, Character, String, Double> listener10 = (
                param1, param2, param3, param4, param5, param6, param7, param8, param9, param10) -> {
                    assertNull(param1);
                    assertNull(param2);
                    assertNull(param3);
                    assertNull(param4);
                    assertNull(param5);
                    assertNull(param6);
                    assertNull(param7);
                    assertNull(param8);
                    assertNull(param9);
                    assertNull(param10);
                    latches[0].countDown();
                };
        NineParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean, Character, String> listener9 = 
                (param1, param2, param3, param4, param5, param6, param7, param8, param9) -> {
                    assertNull(param1);
                    assertNull(param2);
                    assertNull(param3);
                    assertNull(param4);
                    assertNull(param5);
                    assertNull(param6);
                    assertNull(param7);
                    assertNull(param8);
                    assertNull(param9);
                    latches[1].countDown();
                };
        EightParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean, Character> listener8 = 
                (param1, param2, param3, param4, param5, param6, param7, param8) -> {
                    assertNull(param1);
                    assertNull(param2);
                    assertNull(param3);
                    assertNull(param4);
                    assertNull(param5);
                    assertNull(param6);
                    assertNull(param7);
                    assertNull(param8);
                    latches[2].countDown();
                };
        SevenParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean> listener7 = 
                (param1, param2, param3, param4, param5, param6, param7) -> {
                    assertNull(param1);
                    assertNull(param2);
                    assertNull(param3);
                    assertNull(param4);
                    assertNull(param5);
                    assertNull(param6);
                    assertNull(param7);
                    latches[3].countDown();
                };
        SixParameterConsumer<String, Integer, Double, Long, Float, Byte> listener6 = 
                (param1, param2, param3, param4, param5, param6) -> {
                    assertNull(param1);
                    assertNull(param2);
                    assertNull(param3);
                    assertNull(param4);
                    assertNull(param5);
                    assertNull(param6);
                    latches[4].countDown();
                };
        FiveParameterConsumer<String, Integer, Double, Long, Float> listener5 = 
                (param1, param2, param3, param4, param5) -> {
                    assertNull(param1);
                    assertNull(param2);
                    assertNull(param3);
                    assertNull(param4);
                    assertNull(param5);
                    latches[5].countDown();
                };
        FourParameterConsumer<String, Integer, Double, Long> listener4 = (param1, param2, param3, param4) -> {
            assertNull(param1);
            assertNull(param2);
            assertNull(param3);
            assertNull(param4);
            latches[6].countDown();
        };
        ThreeParameterConsumer<String, Integer, Double> listener3 = (param1, param2, param3) -> {
            assertNull(param1);
            assertNull(param2);
            assertNull(param3);
            latches[7].countDown();
        };
        TwoParameterConsumer<String, Integer> listener2 = (param1, param2) -> {
            assertNull(param1);
            assertNull(param2);
            latches[8].countDown();
        };
        SingleParameterConsumer<String> listener1 = param1 -> {
            assertNull(param1);
            latches[9].countDown();
        };
        Consumer<Object[]> genericListener = data -> {
            assertNotNull(data);
            assertThat(data.length, is(0));
            latches[10].countDown();
        };
        NoParameterConsumer noParamsListener = () -> latches[11].countDown();

        EventBus eventBus = new EventBus("id",
                new EventBusConfig(BackpressureStrategy.BUFFER, false), new Metrics());

        eventBus.on("e").subscribe(noParamsListener);
        eventBus.on("e").subscribe(genericListener);
        eventBus.on("e").subscribe(listener1);
        eventBus.on("e").subscribe(listener2);
        eventBus.on("e").subscribe(listener3);
        eventBus.on("e").subscribe(listener4);
        eventBus.on("e").subscribe(listener5);
        eventBus.on("e").subscribe(listener6);
        eventBus.on("e").subscribe(listener7);
        eventBus.on("e").subscribe(listener8);
        eventBus.on("e").subscribe(listener9);
        eventBus.on("e").subscribe(listener10);

        eventBus.emit("e");

        for (CountDownLatch latch : latches) {
            latch.await(100, TimeUnit.MILLISECONDS);
            assertThat(latch.getCount(),is(0L));
        }
    }

    @Test
    void testParameterCastingAndPassing() throws Exception {
        final CountDownLatch[] latches = new CountDownLatch[12];
        for (int i = 0; i < latches.length; i++) {
            latches[i] = new CountDownLatch(1);
        }

        final String p1 = "1";
        final Integer p2 = 2;
        final Double p3 = 3.0;
        final Long p4 = 4L;
        final Float p5 = 5.0f;
        final Byte p6 = 6;
        final Boolean p7 = Boolean.TRUE;
        final Character p8 = '8';
        final String p9 = "9";
        final Double p10 = 10.0;

        TenParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean, Character, String, Double> listener10 = (
                param1, param2, param3, param4, param5, param6, param7, param8, param9, param10) -> {
            assertSame(param1, p1);
            assertSame(param2, p2);
            assertSame(param3, p3);
            assertSame(param4, p4);
            assertSame(param5, p5);
            assertSame(param6, p6);
            assertSame(param7, p7);
            assertSame(param8, p8);
            assertSame(param9, p9);
            assertSame(param10, p10);
            latches[0].countDown();
        };

        NineParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean, Character, String> listener9 = (
                param1, param2, param3, param4, param5, param6, param7, param8, param9) -> {
            assertSame(param1, p1);
            assertSame(param2, p2);
            assertSame(param3, p3);
            assertSame(param4, p4);
            assertSame(param5, p5);
            assertSame(param6, p6);
            assertSame(param7, p7);
            assertSame(param8, p8);
            assertSame(param9, p9);
            latches[1].countDown();
        };

        EightParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean, Character> listener8 = (
                param1, param2, param3, param4, param5, param6, param7, param8) -> {
            assertSame(param1, p1);
            assertSame(param2, p2);
            assertSame(param3, p3);
            assertSame(param4, p4);
            assertSame(param5, p5);
            assertSame(param6, p6);
            assertSame(param7, p7);
            assertSame(param8, p8);
            latches[2].countDown();
        };

        SevenParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean> listener7 = (param1, param2,
                param3, param4, param5, param6, param7) -> {
            assertSame(param1, p1);
            assertSame(param2, p2);
            assertSame(param3, p3);
            assertSame(param4, p4);
            assertSame(param5, p5);
            assertSame(param6, p6);
            assertSame(param7, p7);
            latches[3].countDown();
        };

        SixParameterConsumer<String, Integer, Double, Long, Float, Byte> listener6 = (param1, param2, param3,
                param4, param5, param6) -> {
            assertSame(param1, p1);
            assertSame(param2, p2);
            assertSame(param3, p3);
            assertSame(param4, p4);
            assertSame(param5, p5);
            assertSame(param6, p6);
            latches[4].countDown();
        };

        FiveParameterConsumer<String, Integer, Double, Long, Float> listener5 = (param1, param2, param3, param4,
                param5) -> {
            assertSame(param1, p1);
            assertSame(param2, p2);
            assertSame(param3, p3);
            assertSame(param4, p4);
            assertSame(param5, p5);
            latches[5].countDown();
        };

        FourParameterConsumer<String, Integer, Double, Long> listener4 = (param1, param2, param3, param4) -> {
            assertSame(param1, p1);
            assertSame(param2, p2);
            assertSame(param3, p3);
            assertSame(param4, p4);
            latches[6].countDown();
        };

        ThreeParameterConsumer<String, Integer, Double> listener3 = (param1, param2, param3) -> {
            assertSame(param1, p1);
            assertSame(param2, p2);
            assertSame(param3, p3);
            latches[7].countDown();
        };

        TwoParameterConsumer<String, Integer> listener2 = (param1, param2) -> {
            assertSame(param1, p1);
            assertSame(param2, p2);
            latches[8].countDown();
        };

        SingleParameterConsumer<String> listener1 = param1 -> {
            assertSame(param1, p1);
            latches[9].countDown();
        };

        Consumer<Object[]> genericListener = data -> {
            assertSame(data[0], p1);
            assertSame(data[1], p2);
            assertSame(data[2], p3);
            assertSame(data[3], p4);
            assertSame(data[4], p5);
            assertSame(data[5], p6);
            assertSame(data[6], p7);
            assertSame(data[7], p8);
            assertSame(data[8], p9);
            assertSame(data[9], p10);
            latches[10].countDown();
        };
        NoParameterConsumer noParamsListener = () -> latches[11].countDown();

        EventBus eventBus = new EventBus("id",
                new EventBusConfig(BackpressureStrategy.BUFFER, false), new Metrics());

        eventBus.on("e").subscribe(noParamsListener);
        eventBus.on("e").subscribe(genericListener);
        eventBus.on("e").subscribe(listener1);
        eventBus.on("e").subscribe(listener2);
        eventBus.on("e").subscribe(listener3);
        eventBus.on("e").subscribe(listener4);
        eventBus.on("e").subscribe(listener5);
        eventBus.on("e").subscribe(listener6);
        eventBus.on("e").subscribe(listener7);
        eventBus.on("e").subscribe(listener8);
        eventBus.on("e").subscribe(listener9);
        eventBus.on("e").subscribe(listener10);

        eventBus.emit("e", p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);

        for (CountDownLatch latch : latches) {
            latch.await(100, TimeUnit.MILLISECONDS);
            assertThat(latch.getCount(),is(0L));
        }

    }
}
