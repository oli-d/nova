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

import ch.squaredesk.nova.events.EventLoopConfig;
import ch.squaredesk.nova.events.EventLoop;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.functions.Consumer;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ConsumerWrappersTest {
	@Test
	public void testInvokingWithoutParams() {
		final boolean[] listenersInvoked = new boolean[12];

		TenParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean, Character, String, Double> listener10 = (
				param1, param2, param3, param4, param5, param6, param7, param8, param9, param10) -> {
					listenersInvoked[0] = true;
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
				};
        NineParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean, Character, String> listener9 = 
                (param1, param2, param3, param4, param5, param6, param7, param8, param9) -> {
                    listenersInvoked[1] = true;
                    assertNull(param1);
                    assertNull(param2);
                    assertNull(param3);
                    assertNull(param4);
                    assertNull(param5);
                    assertNull(param6);
                    assertNull(param7);
                    assertNull(param8);
                    assertNull(param9);
                };
        EightParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean, Character> listener8 = 
                (param1, param2, param3, param4, param5, param6, param7, param8) -> {
                    listenersInvoked[2] = true;
                    assertNull(param1);
                    assertNull(param2);
                    assertNull(param3);
                    assertNull(param4);
                    assertNull(param5);
                    assertNull(param6);
                    assertNull(param7);
                    assertNull(param8);
                };
        SevenParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean> listener7 = 
                (param1, param2, param3, param4, param5, param6, param7) -> {
                    listenersInvoked[3] = true;
                    assertNull(param1);
                    assertNull(param2);
                    assertNull(param3);
                    assertNull(param4);
                    assertNull(param5);
                    assertNull(param6);
                    assertNull(param7);
                };
        SixParameterConsumer<String, Integer, Double, Long, Float, Byte> listener6 = 
                (param1, param2, param3, param4, param5, param6) -> {
                    listenersInvoked[4] = true;
                    assertNull(param1);
                    assertNull(param2);
                    assertNull(param3);
                    assertNull(param4);
                    assertNull(param5);
                    assertNull(param6);
                };
        FiveParameterConsumer<String, Integer, Double, Long, Float> listener5 = 
                (param1, param2, param3, param4, param5) -> {
                    listenersInvoked[5] = true;
                    assertNull(param1);
                    assertNull(param2);
                    assertNull(param3);
                    assertNull(param4);
                    assertNull(param5);
                };
        FourParameterConsumer<String, Integer, Double, Long> listener4 = (param1, param2, param3, param4) -> {
            listenersInvoked[6] = true;
            assertNull(param1);
            assertNull(param2);
            assertNull(param3);
            assertNull(param4);
        };
        ThreeParameterConsumer<String, Integer, Double> listener3 = (param1, param2, param3) -> {
            listenersInvoked[7] = true;
            assertNull(param1);
            assertNull(param2);
            assertNull(param3);
        };
        TwoParameterConsumer<String, Integer> listener2 = (param1, param2) -> {
            listenersInvoked[8] = true;
            assertNull(param1);
            assertNull(param2);
        };
        SingleParameterConsumer<String> listener1 = param1 -> {
            listenersInvoked[9] = true;
            assertNull(param1);
        };
        Consumer<Object[]> genericListener = data -> {
            listenersInvoked[10] = true;
            assertNotNull(data);
            assertThat(data.length, is(0));
        };
        NoParameterConsumer noParamsListener = () -> listenersInvoked[11] = true;

        EventLoopConfig eventLoopConfig =
                EventLoopConfig.builder()
                        .setDispatchThreadStrategy(EventLoopConfig.DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD)
                        .build();
        EventLoop eventLoop = new EventLoop("id", eventLoopConfig, new Metrics());

        eventLoop.observe("e").subscribe(noParamsListener);
        eventLoop.observe("e").subscribe(genericListener);
        eventLoop.observe("e").subscribe(listener1);
        eventLoop.observe("e").subscribe(listener2);
        eventLoop.observe("e").subscribe(listener3);
        eventLoop.observe("e").subscribe(listener4);
        eventLoop.observe("e").subscribe(listener5);
        eventLoop.observe("e").subscribe(listener6);
        eventLoop.observe("e").subscribe(listener7);
        eventLoop.observe("e").subscribe(listener8);
        eventLoop.observe("e").subscribe(listener9);
        eventLoop.observe("e").subscribe(listener10);

        eventLoop.emit("e");

        for (boolean aListenersInvoked : listenersInvoked) {
            assertTrue(aListenersInvoked);
        }
	}

	@Test
	public void testParameterCastingAndPassing() {
		final boolean[] listenersInvoked = new boolean[12];

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
			listenersInvoked[0] = true;
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
		};

		NineParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean, Character, String> listener9 = (
				param1, param2, param3, param4, param5, param6, param7, param8, param9) -> {
			listenersInvoked[1] = true;
			assertSame(param1, p1);
			assertSame(param2, p2);
			assertSame(param3, p3);
			assertSame(param4, p4);
			assertSame(param5, p5);
			assertSame(param6, p6);
			assertSame(param7, p7);
			assertSame(param8, p8);
			assertSame(param9, p9);
		};

		EightParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean, Character> listener8 = (
				param1, param2, param3, param4, param5, param6, param7, param8) -> {
			listenersInvoked[2] = true;
			assertSame(param1, p1);
			assertSame(param2, p2);
			assertSame(param3, p3);
			assertSame(param4, p4);
			assertSame(param5, p5);
			assertSame(param6, p6);
			assertSame(param7, p7);
			assertSame(param8, p8);
		};

		SevenParameterConsumer<String, Integer, Double, Long, Float, Byte, Boolean> listener7 = (param1, param2,
				param3, param4, param5, param6, param7) -> {
			listenersInvoked[3] = true;
			assertSame(param1, p1);
			assertSame(param2, p2);
			assertSame(param3, p3);
			assertSame(param4, p4);
			assertSame(param5, p5);
			assertSame(param6, p6);
			assertSame(param7, p7);
		};

		SixParameterConsumer<String, Integer, Double, Long, Float, Byte> listener6 = (param1, param2, param3,
				param4, param5, param6) -> {
			listenersInvoked[4] = true;
			assertSame(param1, p1);
			assertSame(param2, p2);
			assertSame(param3, p3);
			assertSame(param4, p4);
			assertSame(param5, p5);
			assertSame(param6, p6);
		};

		FiveParameterConsumer<String, Integer, Double, Long, Float> listener5 = (param1, param2, param3, param4,
				param5) -> {
			listenersInvoked[5] = true;
			assertSame(param1, p1);
			assertSame(param2, p2);
			assertSame(param3, p3);
			assertSame(param4, p4);
			assertSame(param5, p5);
		};

		FourParameterConsumer<String, Integer, Double, Long> listener4 = (param1, param2, param3, param4) -> {
			listenersInvoked[6] = true;
			assertSame(param1, p1);
			assertSame(param2, p2);
			assertSame(param3, p3);
			assertSame(param4, p4);
		};

		ThreeParameterConsumer<String, Integer, Double> listener3 = (param1, param2, param3) -> {
			listenersInvoked[7] = true;
			assertSame(param1, p1);
			assertSame(param2, p2);
			assertSame(param3, p3);
		};

		TwoParameterConsumer<String, Integer> listener2 = (param1, param2) -> {
			listenersInvoked[8] = true;
			assertSame(param1, p1);
			assertSame(param2, p2);
		};

		SingleParameterConsumer<String> listener1 = param1 -> {
			listenersInvoked[9] = true;
			assertSame(param1, p1);
		};

		Consumer<Object[]> genericListener = data -> {
            listenersInvoked[10] = true;
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
        };
		NoParameterConsumer noParamsListener = () -> listenersInvoked[11] = true;

        EventLoopConfig eventLoopConfig =
                EventLoopConfig.builder()
                        .setDispatchThreadStrategy(EventLoopConfig.DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD)
                        .build();
        EventLoop eventLoop = new EventLoop("id", eventLoopConfig, new Metrics());

		eventLoop.observe("e").subscribe(noParamsListener);
		eventLoop.observe("e").subscribe(genericListener);
		eventLoop.observe("e").subscribe(listener1);
		eventLoop.observe("e").subscribe(listener2);
		eventLoop.observe("e").subscribe(listener3);
		eventLoop.observe("e").subscribe(listener4);
		eventLoop.observe("e").subscribe(listener5);
		eventLoop.observe("e").subscribe(listener6);
		eventLoop.observe("e").subscribe(listener7);
		eventLoop.observe("e").subscribe(listener8);
		eventLoop.observe("e").subscribe(listener9);
		eventLoop.observe("e").subscribe(listener10);

		eventLoop.emit("e", p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);

		for (boolean aListenersInvoked : listenersInvoked) {
			assertTrue(aListenersInvoked);
		}

	}
}
