package com.dotc.nova.events;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.dotc.nova.ProcessingLoop;

public class AsyncEventEmitterTest extends EventEmitterTestBase {

	@Before
	public void setup() {
		ProcessingLoop processingLoop = new ProcessingLoop();
		processingLoop.init();
		eventEmitter = new AsyncEventEmitter(processingLoop);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetListenersWithNullEventThrows() {
		((AsyncEventEmitter) eventEmitter).getListeners(null);
	}

	@Test
	public void testGetListenersWithUnknwonEventReturnsEmptyList() {
		assertNotNull(((AsyncEventEmitter) eventEmitter).getListeners(String.class));
		assertTrue(((AsyncEventEmitter) eventEmitter).getListeners(String.class).isEmpty());
	}

	@Test
	public void testRegisteredListenersCanBeRetrieved() {
		EventListener<String> listener1 = createMock(EventListener.class);
		EventListener<String> listener2 = createMock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		List<EventListener> listeners = ((AsyncEventEmitter) eventEmitter).getListeners(String.class);
		assertNotNull(listeners);
		assertThat(listeners.size(), is(2));
		assertTrue(listeners.contains(listener1));
		assertTrue(listeners.contains(listener2));
	}

}
