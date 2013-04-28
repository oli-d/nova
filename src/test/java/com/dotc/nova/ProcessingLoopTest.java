package com.dotc.nova;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.dotc.nova.events.EventHandler;

public class ProcessingLoopTest {
	private ProcessingLoop processingLoop;

	@Before
	public void setUp() {
		processingLoop = new ProcessingLoop();
		processingLoop.init();
	}

	private EventHandler<String>[] createListeners(final CountDownLatch countDownLatch) {
		EventHandler<String>[] listenersArray = new EventHandler[(int) countDownLatch.getCount()];
		for (int i = 0; i < listenersArray.length; i++) {
			listenersArray[i] = new EventHandler<String>() {

				@Override
				public void handle(String... data) {
					countDownLatch.countDown();
				}
			};
		}
		return listenersArray;
	}

	@Test
	public void testDispatchEventWithListenerArray() {
		int numberOfListeners = 5;
		final CountDownLatch countDownLatch = new CountDownLatch(numberOfListeners);
		EventHandler<String>[] listenersArray = createListeners(countDownLatch);

		processingLoop.dispatch("Test", listenersArray);

		try {
			countDownLatch.await(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		assertThat(countDownLatch.getCount(), is(0l));
	}

	@Test
	public void testDispatchEventWithListenerList() {
		int numberOfListeners = 5;
		final CountDownLatch countDownLatch = new CountDownLatch(numberOfListeners);
		EventHandler<String>[] listenersArray = createListeners(countDownLatch);

		ArrayList<EventHandler> list = new ArrayList<EventHandler>(Arrays.asList(listenersArray));
		processingLoop.dispatch("Test", list);

		try {
			countDownLatch.await(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		assertThat(countDownLatch.getCount(), is(0l));
	}

	@Test
	public void testDispatchHandlerWithoutEvent() {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		EventHandler r = new EventHandler() {
			@Override
			public void handle(Object... data) {
				countDownLatch.countDown();
			}
		};

		processingLoop.dispatch(r);

		try {
			countDownLatch.await(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}

		assertThat(countDownLatch.getCount(), is(0l));
	}

}
