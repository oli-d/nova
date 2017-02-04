/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.timers;

public class TimersTest {
	/*
	@Test(expected = IllegalArgumentException.class)
	public void testSetTimeoutThrowsIfNoCallbackProvided() {
		new Timers(null).setTimeout(null, 100);
	}

	@Test
	public void testSetTimeout() throws Throwable {
		BasicConfigurator.configure();

		CountDownLatch countDownLatch = new CountDownLatch(1);
		boolean[] invocationFlag = new boolean[1];
		Runnable callback = () -> {
			invocationFlag[0] = true;
			countDownLatch.countDown();
		};

		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
                new NoopEventMetricsCollector());

		Timers timers = new Timers(eventLoop);

		long startDelay = 300;
		assertNotNull(timers.setTimeout(callback, startDelay, TimeUnit.MILLISECONDS));

		countDownLatch.await(500, TimeUnit.MILLISECONDS);
		assertThat(invocationFlag[0],is(true));
	}

	@Test
	public void testClearTimeout() throws Exception {
		final int[] counter = new int[1];
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
				new NoopEventMetricsCollector());
		Timers timers = new Timers(eventLoop);

		Runnable callback = () -> {
				counter[0]++;
		};

		long startDelay = 200;
		String id = timers.setTimeout(callback, startDelay);
		timers.clearTimeout(id);
		Thread.sleep(2 * startDelay);
		assertNotNull(id);

		assertThat(counter[0], is(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testClearTimeoutWithNullIdThrows() throws Exception {
		new Timers(null).clearTimeout(null);
	}

	@Test
	public void testClearTimeoutCanBeInvokedWithoutProblemsWithUnknownId() throws Exception {
		new Timers(null).clearTimeout("id");
	}

	@Test
	public void testClearTimeoutCanBeInvokedMultipleTimes() throws Exception {
		final int[] counter = new int[1];
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
                new NoopEventMetricsCollector()) {

			@Override
			public void dispatch(EventListener h) {
				counter[0]++;
			}

		};
		Timers timers = new Timers(eventLoop);

		Runnable callback = () -> {
		};

		long startDelay = 200;
		String id = timers.setTimeout(callback, startDelay);
		timers.clearTimeout(id);
		timers.clearTimeout(id);
		timers.clearTimeout(id);
		Thread.sleep(2 * startDelay);
		assertNotNull(id);

		assertThat(counter[0], is(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetIntervalThrowsIfNoCallbackProvided() {
		new Timers(null).setInterval(null, 100);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetIntervalThrowsIfNoTimeUnitProvided() {
		new Timers(null).setInterval(() -> { }, 100L, null);
	}

	@Test
	public void testSetInterval() throws Throwable {
		final int[] counter = new int[1];
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
                new NoopEventMetricsCollector()) {

			@Override
			public void dispatch(EventListener h) {
				counter[0]++;
			}

		};
		Timers timers = new Timers(eventLoop);

		Runnable callback = () -> {
		};

		long startDelay = 200;
		String id = timers.setInterval(callback, startDelay);
		assertNotNull(id);
		Thread.sleep((4 * startDelay) + 100);
		timers.clearInterval(id);

		assertThat(counter[0], is(4));
	}

	@Test
	public void testClearInterval() throws Exception {
		final int[] counter = new int[1];
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
                new NoopEventMetricsCollector()) {

			@Override
			public void dispatch(EventListener h) {
				counter[0]++;
			}

		};
		Timers timers = new Timers(eventLoop);

		Runnable callback = () -> {
		};

		long startDelay = 200;
		String id = timers.setInterval(callback, startDelay);
		timers.clearInterval(id);
		Thread.sleep(2 * startDelay);
		assertNotNull(id);

		assertThat(counter[0], is(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testClearIntervalWithNullIdThrows() throws Exception {
		new Timers(null).clearInterval(null);
	}

	@Test
	public void testClearIntervalCanBeInvokedWithoutProblemsWithUnknownId() throws Exception {
		new Timers(null).clearInterval("id");
	}

	@Test
	public void testClearIntervalCanBeInvokedMultipleTimes() throws Exception {
		final int[] counter = new int[1];
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
                new NoopEventMetricsCollector()) {

			@Override
			public void dispatch(EventListener h) {
				counter[0]++;
			}

		};
		Timers timers = new Timers(eventLoop);

		Runnable callback = () -> {
		};

		long startDelay = 200;
		String id = timers.setInterval(callback, startDelay);
		timers.clearInterval(id);
		timers.clearInterval(id);
		timers.clearInterval(id);
		Thread.sleep(2 * startDelay);
		assertNotNull(id);

		assertThat(counter[0], is(0));
	}
	*/
}
