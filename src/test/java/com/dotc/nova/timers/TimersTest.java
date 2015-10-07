package com.dotc.nova.timers;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import com.dotc.nova.events.metrics.NoopRunnableTimer;
import com.dotc.nova.events.metrics.RunnableTimer;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.dotc.nova.events.EventDispatchConfig;
import com.dotc.nova.events.EventListener;
import com.dotc.nova.events.EventLoop;
import com.dotc.nova.events.metrics.NoopEventMetricsCollector;

public class TimersTest {

	@Test(expected = IllegalArgumentException.class)
	public void testSetTimeoutThrowsIfNoCallbackProvided() {
		new Timers(null).setTimeout(null, 100);
	}

	@Test
	public void testSetTimeout() throws Throwable {
		BasicConfigurator.configure();

		Runnable callback = mock(Runnable.class);
		EventLoop eventLoop = mock(EventLoop.class);

		Timers timers = new Timers(eventLoop);

		long startDelay = 300;
		assertNotNull(timers.setTimeout(callback, startDelay, TimeUnit.MILLISECONDS));

		ArgumentCaptor<EventListener> eventListenerCaptor = ArgumentCaptor.forClass(EventListener.class);
		verify(eventLoop, timeout(500)).dispatch(eventListenerCaptor.capture());
		assertNotNull(eventListenerCaptor.getValue());
	}

	@Test
	public void testClearTimeout() throws Exception {
		final int[] counter = new int[1];
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
				new NoopEventMetricsCollector(), new NoopRunnableTimer()) {

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
                new NoopEventMetricsCollector(), new NoopRunnableTimer()) {

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
		new Timers(null).setInterval(new Runnable() {
			public void run() {
			}
		}, 100l, null);
	}

	@Test
	public void testSetInterval() throws Throwable {
		final int[] counter = new int[1];
		EventLoop eventLoop = new EventLoop("test", new EventDispatchConfig.Builder().build(),
                new NoopEventMetricsCollector(), new NoopRunnableTimer()) {

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
                new NoopEventMetricsCollector(), new NoopRunnableTimer()) {

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
                new NoopEventMetricsCollector(), new NoopRunnableTimer()) {

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

}
