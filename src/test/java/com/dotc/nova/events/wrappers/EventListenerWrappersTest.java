package com.dotc.nova.events.wrappers;

import org.junit.Assert;
import org.junit.Test;

import com.dotc.nova.events.*;
import com.dotc.nova.events.metrics.NoopEventMetricsCollector;

public class EventListenerWrappersTest {
	@Test
	public void testInvokingWithoutParams() {
		final boolean[] listenersInvoked = new boolean[12];

		TenParametersEventListener<String, Integer, Double, Long, Float, Byte, Boolean, Character, String, Double> listener10 = (
				param1, param2, param3, param4, param5, param6, param7, param8, param9, param10) -> {
					listenersInvoked[0] = true;
					Assert.assertNull(param1);
					Assert.assertNull(param2);
					Assert.assertNull(param3);
					Assert.assertNull(param4);
					Assert.assertNull(param5);
					Assert.assertNull(param6);
					Assert.assertNull(param7);
					Assert.assertNull(param8);
					Assert.assertNull(param9);
					Assert.assertNull(param10);
				};

				NineParametersEventListener<String, Integer, Double, Long, Float, Byte, Boolean, Character, String> listener9 = (
				param1, param2, param3, param4, param5, param6, param7, param8, param9) -> {
							listenersInvoked[1] = true;
							Assert.assertNull(param1);
							Assert.assertNull(param2);
							Assert.assertNull(param3);
							Assert.assertNull(param4);
							Assert.assertNull(param5);
							Assert.assertNull(param6);
							Assert.assertNull(param7);
							Assert.assertNull(param8);
							Assert.assertNull(param9);
						};
						EightParametersEventListener<String, Integer, Double, Long, Float, Byte, Boolean, Character> listener8 = (
				param1, param2, param3, param4, param5, param6, param7, param8) -> {
									listenersInvoked[2] = true;
									Assert.assertNull(param1);
									Assert.assertNull(param2);
									Assert.assertNull(param3);
									Assert.assertNull(param4);
									Assert.assertNull(param5);
									Assert.assertNull(param6);
									Assert.assertNull(param7);
									Assert.assertNull(param8);
								};
								SevenParametersEventListener<String, Integer, Double, Long, Float, Byte, Boolean> listener7 = (param1, param2,
				param3, param4, param5, param6, param7) -> {
											listenersInvoked[3] = true;
											Assert.assertNull(param1);
											Assert.assertNull(param2);
											Assert.assertNull(param3);
											Assert.assertNull(param4);
											Assert.assertNull(param5);
											Assert.assertNull(param6);
											Assert.assertNull(param7);
										};
										SixParametersEventListener<String, Integer, Double, Long, Float, Byte> listener6 = (param1, param2, param3,
				param4, param5, param6) -> {
											listenersInvoked[4] = true;
											Assert.assertNull(param1);
											Assert.assertNull(param2);
											Assert.assertNull(param3);
											Assert.assertNull(param4);
											Assert.assertNull(param5);
											Assert.assertNull(param6);
										};
										FiveParametersEventListener<String, Integer, Double, Long, Float> listener5 = (param1, param2, param3, param4,
				param5) -> {
											listenersInvoked[5] = true;
											Assert.assertNull(param1);
											Assert.assertNull(param2);
											Assert.assertNull(param3);
											Assert.assertNull(param4);
											Assert.assertNull(param5);
										};
										FourParametersEventListener<String, Integer, Double, Long> listener4 = (param1, param2, param3, param4) -> {
											listenersInvoked[6] = true;
											Assert.assertNull(param1);
											Assert.assertNull(param2);
											Assert.assertNull(param3);
											Assert.assertNull(param4);
										};
										ThreeParametersEventListener<String, Integer, Double> listener3 = (param1, param2, param3) -> {
											listenersInvoked[7] = true;
											Assert.assertNull(param1);
											Assert.assertNull(param2);
											Assert.assertNull(param3);
										};
										TwoParametersEventListener<String, Integer> listener2 = (param1, param2) -> {
											listenersInvoked[8] = true;
											Assert.assertNull(param1);
											Assert.assertNull(param2);
										};
										SingleParameterEventListener<String> listener1 = param1 -> {
											listenersInvoked[9] = true;
											Assert.assertNull(param1);
										};
										EventListener genericListener = new EventListener() {
											@Override
											public void handle(Object... data) {
												listenersInvoked[10] = true;
												Assert.assertNull(data);
											}
										};
										NoParameterEventListener noParamsListener = () -> {
											listenersInvoked[11] = true;
										};

										EventEmitter ee = new CurrentThreadEventEmitter(false, new NoopEventMetricsCollector());

										ee.on("e", noParamsListener);
										ee.on("e", genericListener);
										ee.on("e", listener1);
										ee.on("e", listener2);
										ee.on("e", listener3);
										ee.on("e", listener4);
										ee.on("e", listener5);
										ee.on("e", listener6);
										ee.on("e", listener7);
										ee.on("e", listener8);
										ee.on("e", listener9);
										ee.on("e", listener10);

										ee.emit("e");

										for (int i = 0; i < listenersInvoked.length; i++) {
											Assert.assertTrue(listenersInvoked[i]);
										}

	}

	@Test
	public void testParameterCastingAndPassing() {
		final boolean[] listenersInvoked = new boolean[12];

		final String p1 = "1";
		final Integer p2 = 2;
		final Double p3 = 3.0;
		final Long p4 = 4l;
		final Float p5 = 5.0f;
		final Byte p6 = 6;
		final Boolean p7 = Boolean.TRUE;
		final Character p8 = '8';
		final String p9 = "9";
		final Double p10 = 10.0;

		TenParametersEventListener<String, Integer, Double, Long, Float, Byte, Boolean, Character, String, Double> listener10 = (
				param1, param2, param3, param4, param5, param6, param7, param8, param9, param10) -> {
			listenersInvoked[0] = true;
			Assert.assertSame(param1, p1);
			Assert.assertSame(param2, p2);
			Assert.assertSame(param3, p3);
			Assert.assertSame(param4, p4);
			Assert.assertSame(param5, p5);
			Assert.assertSame(param6, p6);
			Assert.assertSame(param7, p7);
			Assert.assertSame(param8, p8);
			Assert.assertSame(param9, p9);
			Assert.assertSame(param10, p10);
		};

		NineParametersEventListener<String, Integer, Double, Long, Float, Byte, Boolean, Character, String> listener9 = (
				param1, param2, param3, param4, param5, param6, param7, param8, param9) -> {
			listenersInvoked[1] = true;
			Assert.assertSame(param1, p1);
			Assert.assertSame(param2, p2);
			Assert.assertSame(param3, p3);
			Assert.assertSame(param4, p4);
			Assert.assertSame(param5, p5);
			Assert.assertSame(param6, p6);
			Assert.assertSame(param7, p7);
			Assert.assertSame(param8, p8);
			Assert.assertSame(param9, p9);
		};

		EightParametersEventListener<String, Integer, Double, Long, Float, Byte, Boolean, Character> listener8 = (
				param1, param2, param3, param4, param5, param6, param7, param8) -> {
			listenersInvoked[2] = true;
			Assert.assertSame(param1, p1);
			Assert.assertSame(param2, p2);
			Assert.assertSame(param3, p3);
			Assert.assertSame(param4, p4);
			Assert.assertSame(param5, p5);
			Assert.assertSame(param6, p6);
			Assert.assertSame(param7, p7);
			Assert.assertSame(param8, p8);
		};

		SevenParametersEventListener<String, Integer, Double, Long, Float, Byte, Boolean> listener7 = (param1, param2,
				param3, param4, param5, param6, param7) -> {
			listenersInvoked[3] = true;
			Assert.assertSame(param1, p1);
			Assert.assertSame(param2, p2);
			Assert.assertSame(param3, p3);
			Assert.assertSame(param4, p4);
			Assert.assertSame(param5, p5);
			Assert.assertSame(param6, p6);
			Assert.assertSame(param7, p7);
		};

		SixParametersEventListener<String, Integer, Double, Long, Float, Byte> listener6 = (param1, param2, param3,
				param4, param5, param6) -> {
			listenersInvoked[4] = true;
			Assert.assertSame(param1, p1);
			Assert.assertSame(param2, p2);
			Assert.assertSame(param3, p3);
			Assert.assertSame(param4, p4);
			Assert.assertSame(param5, p5);
			Assert.assertSame(param6, p6);
		};

		FiveParametersEventListener<String, Integer, Double, Long, Float> listener5 = (param1, param2, param3, param4,
				param5) -> {
			listenersInvoked[5] = true;
			Assert.assertSame(param1, p1);
			Assert.assertSame(param2, p2);
			Assert.assertSame(param3, p3);
			Assert.assertSame(param4, p4);
			Assert.assertSame(param5, p5);
		};

		FourParametersEventListener<String, Integer, Double, Long> listener4 = (param1, param2, param3, param4) -> {
			listenersInvoked[6] = true;
			Assert.assertSame(param1, p1);
			Assert.assertSame(param2, p2);
			Assert.assertSame(param3, p3);
			Assert.assertSame(param4, p4);
		};

		ThreeParametersEventListener<String, Integer, Double> listener3 = (param1, param2, param3) -> {
			listenersInvoked[7] = true;
			Assert.assertSame(param1, p1);
			Assert.assertSame(param2, p2);
			Assert.assertSame(param3, p3);
		};

		TwoParametersEventListener<String, Integer> listener2 = (param1, param2) -> {
			listenersInvoked[8] = true;
			Assert.assertSame(param1, p1);
			Assert.assertSame(param2, p2);
		};

		SingleParameterEventListener<String> listener1 = param1 -> {
			listenersInvoked[9] = true;
			Assert.assertSame(param1, p1);
		};

		EventListener genericListener = new EventListener() {
			@Override
			public void handle(Object... data) {
				listenersInvoked[10] = true;
				Assert.assertSame(data[0], p1);
				Assert.assertSame(data[1], p2);
				Assert.assertSame(data[2], p3);
				Assert.assertSame(data[3], p4);
				Assert.assertSame(data[4], p5);
				Assert.assertSame(data[5], p6);
				Assert.assertSame(data[6], p7);
				Assert.assertSame(data[7], p8);
				Assert.assertSame(data[8], p9);
				Assert.assertSame(data[9], p10);
			}
		};
		NoParameterEventListener noParamsListener = () -> listenersInvoked[11] = true;

		EventEmitter ee = new CurrentThreadEventEmitter(false, new NoopEventMetricsCollector());

		ee.on("e", noParamsListener);
		ee.on("e", genericListener);
		ee.on("e", listener1);
		ee.on("e", listener2);
		ee.on("e", listener3);
		ee.on("e", listener4);
		ee.on("e", listener5);
		ee.on("e", listener6);
		ee.on("e", listener7);
		ee.on("e", listener8);
		ee.on("e", listener9);
		ee.on("e", listener10);

		ee.emit("e", p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);

		for (int i = 0; i < listenersInvoked.length; i++) {
			Assert.assertTrue(listenersInvoked[i]);
		}

	}
}
