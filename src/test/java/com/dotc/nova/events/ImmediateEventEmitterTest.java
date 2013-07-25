package com.dotc.nova.events;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

public class ImmediateEventEmitterTest {
	private EventEmitter eventEmitter;

	@Before
	public void setup() {
		eventEmitter = new SyncEventEmitter();
	}

	@Test
	public void testAllListenerCalledDuringDispatchingAlthoughOneMightThrow() {
		EventListener<String> listener1 = mock(EventListener.class);
		EventListener<String> listener2 = mock(EventListener.class);
		EventListener<String> listener3 = mock(EventListener.class);

		doThrow(new RuntimeException("For test")).when(listener2).handle("Second");

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);
		eventEmitter.on(String.class, listener3);

		eventEmitter.emit(String.class, "First");
		eventEmitter.emit(String.class, "Second");

		verify(listener1).handle("First");
		verify(listener1).handle("Second");
		verify(listener2).handle("First");
		verify(listener2).handle("Second");
		verify(listener3).handle("First");
		verify(listener3).handle("Second");

		verifyNoMoreInteractions(listener1, listener2, listener3);
	}

}
