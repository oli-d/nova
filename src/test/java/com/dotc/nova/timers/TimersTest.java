package com.dotc.nova.timers;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.easymock.Capture;
import org.junit.Test;

import com.dotc.nova.ProcessingLoop;
import com.dotc.nova.TestHelper;
import com.dotc.nova.events.EventListener;

public class TimersTest {

	@Test(expected = IllegalArgumentException.class)
	public void testSetTimeoutThrowsIfNoCallbackProvided() {
		new Timers(null).setTimeout(null, 100);
	}

	@Test
	public void testSetTimeout() throws Throwable {
		BasicConfigurator.configure();
		Runnable callback = createMock(Runnable.class);

		Capture<EventListener> eventListenerCapture = new Capture<>();
		ProcessingLoop processingLoop = createMock(ProcessingLoop.class);
		processingLoop.dispatch(capture(eventListenerCapture));
		expectLastCall().once();
		replay(callback, processingLoop);

		Timers timers = new Timers(processingLoop);

		long startDelay = 300;
		long checkDelay = 50;
		long maxCheckTime = startDelay + 150;

		assertNotNull(timers.setTimeout(callback, startDelay, TimeUnit.MILLISECONDS));

		assertNotNull(TestHelper.getCaptureValue(eventListenerCapture));

		long endTime = System.currentTimeMillis() + maxCheckTime;
		while (System.currentTimeMillis() <= endTime) {
			Thread.sleep(checkDelay);
			try {
				verify(processingLoop);
				verify(callback);
				return;
			} catch (Throwable t) {
				// noop, unable to verify
			}
		}

		fail("Callback not put on processing loop");
	}

	@Test
	public void testClearTimeout() throws Exception {
		final int[] counter = new int[1];
		ProcessingLoop processingLoop = new ProcessingLoop() {

			@Override
			public void dispatch(EventListener h) {
				counter[0]++;
			}

		};
		Timers timers = new Timers(processingLoop);

		Runnable callback = new Runnable() {

			@Override
			public void run() {
			}

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
		ProcessingLoop processingLoop = new ProcessingLoop() {

			@Override
			public void dispatch(EventListener h) {
				counter[0]++;
			}

		};
		Timers timers = new Timers(processingLoop);

		Runnable callback = new Runnable() {

			@Override
			public void run() {
			}

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
		ProcessingLoop processingLoop = new ProcessingLoop() {

			@Override
			public void dispatch(EventListener h) {
				counter[0]++;
			}

		};
		Timers timers = new Timers(processingLoop);

		Runnable callback = new Runnable() {

			@Override
			public void run() {
			}

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
		ProcessingLoop processingLoop = new ProcessingLoop() {

			@Override
			public void dispatch(EventListener h) {
				counter[0]++;
			}

		};
		Timers timers = new Timers(processingLoop);

		Runnable callback = new Runnable() {

			@Override
			public void run() {
			}

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
		ProcessingLoop processingLoop = new ProcessingLoop() {

			@Override
			public void dispatch(EventListener h) {
				counter[0]++;
			}

		};
		Timers timers = new Timers(processingLoop);

		Runnable callback = new Runnable() {

			@Override
			public void run() {
			}

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
