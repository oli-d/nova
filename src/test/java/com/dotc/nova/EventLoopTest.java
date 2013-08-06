package com.dotc.nova;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.dotc.nova.events.EventListener;

public class EventLoopTest {
	private EventLoop eventLoop;

	@Before
	public void setUp() {
		eventLoop = new EventLoop(new EventDispatchConfig.Builder().build());
	}

	private EventListener<String>[] createListeners(final CountDownLatch countDownLatch) {
		EventListener<String>[] listenersArray = new EventListener[(int) countDownLatch.getCount()];
		for (int i = 0; i < listenersArray.length; i++) {
			listenersArray[i] = new EventListener<String>() {

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
		EventListener<String>[] listenersArray = createListeners(countDownLatch);

		eventLoop.dispatch("Test", listenersArray);

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
		EventListener<String>[] listenersArray = createListeners(countDownLatch);

		ArrayList<EventListener> list = new ArrayList<EventListener>(Arrays.asList(listenersArray));
		eventLoop.dispatch("Test", list, "Data");

		try {
			countDownLatch.await(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		assertThat(countDownLatch.getCount(), is(0l));
	}

	@Test
	public void testDispatchHandlerWithoutEvent() {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		EventListener r = new EventListener() {
			@Override
			public void handle(Object... data) {
				countDownLatch.countDown();
			}
		};

		eventLoop.dispatch(r);

		try {
			countDownLatch.await(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}

		assertThat(countDownLatch.getCount(), is(0l));
	}

}
