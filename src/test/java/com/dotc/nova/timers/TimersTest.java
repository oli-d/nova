package com.dotc.nova.timers;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.dotc.nova.ProcessingLoop;

public class TimersTest {
	private Timers timers;

	@Before
	public void setup() {
		timers = new Timers();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetTimeoutThrowsIfNoCallbackProvided() {
		timers.setTimeout(null, 100);
	}

	@Test
	public void testSetTimeout() throws Throwable {
		Runnable callback = createMock(Runnable.class);

		ProcessingLoop processingLoop = createMock(ProcessingLoop.class);
		processingLoop.dispatch(eq(callback));
		expectLastCall().once();
		replay(callback, processingLoop);

		timers.setProcessingLoop(processingLoop);

		long startDelay = 300;
		long checkDelay = 50;
		long maxCheckTime = startDelay + 150;

		assertNotNull(timers.setTimeout(callback, startDelay));

		boolean verified = false;
		long endTime = System.currentTimeMillis() + maxCheckTime;
		while (!verified && System.currentTimeMillis() <= endTime) {
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
			public void dispatch(Runnable r) {
				counter[0]++;
			}

		};
		timers.setProcessingLoop(processingLoop);

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
		timers.clearTimeout(null);
	}

	@Test
	public void testClearTimeoutCanBeInvokedWithoutProblemsWithUnknownId() throws Exception {
		timers.clearTimeout("id");
	}

	@Test
	public void testClearTimeoutCanBeInvokedMultipleTimes() throws Exception {
		final int[] counter = new int[1];
		ProcessingLoop processingLoop = new ProcessingLoop() {

			@Override
			public void dispatch(Runnable r) {
				counter[0]++;
			}

		};
		timers.setProcessingLoop(processingLoop);

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
		timers.setInterval(null, 100);
	}

	@Test
	public void testSetInterval() throws Throwable {
		final int[] counter = new int[1];
		ProcessingLoop processingLoop = new ProcessingLoop() {

			@Override
			public void dispatch(Runnable r) {
				counter[0]++;
			}

		};
		timers.setProcessingLoop(processingLoop);

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
			public void dispatch(Runnable r) {
				counter[0]++;
			}

		};
		timers.setProcessingLoop(processingLoop);

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
		timers.clearInterval(null);
	}

	@Test
	public void testClearIntervalCanBeInvokedWithoutProblemsWithUnknownId() throws Exception {
		timers.clearInterval("id");
	}

	@Test
	public void testClearIntervalCanBeInvokedMultipleTimes() throws Exception {
		final int[] counter = new int[1];
		ProcessingLoop processingLoop = new ProcessingLoop() {

			@Override
			public void dispatch(Runnable r) {
				counter[0]++;
			}

		};
		timers.setProcessingLoop(processingLoop);

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
