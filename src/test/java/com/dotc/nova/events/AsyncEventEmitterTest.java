package com.dotc.nova.events;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.junit.*;

import com.dotc.nova.ProcessingLoop;

public class AsyncEventEmitterTest {
	private AsyncEventEmitter eventEmitter;

	@Before
	public void setup() {
		ProcessingLoop processingLoop = new ProcessingLoop();
		processingLoop.init();
		eventEmitter = new AsyncEventEmitter(processingLoop);
	}

	@BeforeClass
	public static void initLogging() {
		BasicConfigurator.configure();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisteringNullEventThrows() {
		EventListener<String> listener = mock(EventListener.class);
		eventEmitter.on(null, listener);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisteringNullListenerThrows() {
		eventEmitter.on(String.class, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemovingAllWithNullEventThrows() {
		eventEmitter.removeAllListeners(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemovingWithNullEventThrows() {
		EventListener<String> listener = mock(EventListener.class);
		eventEmitter.removeListener(null, listener);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemovingWithNullListenerThrows() {
		eventEmitter.removeListener(String.class, null);
	}

	@Test
	public void testRemovingNotYetRegisteredListenerIsSilentlyIgnored() {
		EventListener<String> listener = mock(EventListener.class);
		eventEmitter.removeListener(String.class, listener);
	}

	@Test
	public void testListenerCanBeRemoved() {
		EventListener<String> listener1 = mock(EventListener.class);
		EventListener<String> listener2 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		eventEmitter.emit(String.class, "MyEvent1");

		eventEmitter.removeListener(String.class, listener1);
		eventEmitter.emit(String.class, "MyEvent2");

		eventEmitter.removeListener(String.class, listener2);
		eventEmitter.emit(String.class, "MyEvent3");

		verify(listener1, timeout(200)).handle(eq("MyEvent1"));
		verify(listener2, timeout(200)).handle(eq("MyEvent1"));
		verify(listener2, timeout(200)).handle(eq("MyEvent2"));

		verifyNoMoreInteractions(listener1, listener2);
	}

	@Test
	public void testAllListenersCanBeRemoved() {
		EventListener<String> listener1 = mock(EventListener.class);
		EventListener<String> listener2 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		eventEmitter.removeAllListeners(String.class);
		eventEmitter.emit(String.class, "MyEvent1");

		verifyNoMoreInteractions(listener1, listener2);
	}

	@Test
	public void testListenersAreRemovedFromNormalAndOneOffMaps() {
		EventListener<String> listener1 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.once(String.class, listener1);

		eventEmitter.removeListener(String.class, listener1);
		eventEmitter.emit("MyEvent1");

		verifyNoMoreInteractions(listener1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmmittingNullThrows() {
		eventEmitter.emit(null);
	}

	@Test
	public void testRegisteredListenerIsCalledWithoutDataBeingPassed() {
		EventListener<String> listener1 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.emit(String.class);

		verify(listener1, timeout(1000)).handle((String[]) null);
	}

	@Test
	public void testRegisteredListenerCalledEverytimeAnEventIsEmitted() {
		EventListener<String> listener1 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);

		eventEmitter.emit(String.class, "MyEvent1");
		eventEmitter.emit(String.class, "MyEvent2");
		eventEmitter.emit(String.class, "MyEvent3", "MyEvent4");

		verify(listener1, timeout(1000)).handle("MyEvent1");
		verify(listener1, timeout(1000)).handle("MyEvent2");
		verify(listener1, timeout(1000)).handle("MyEvent3", "MyEvent4");
		verifyNoMoreInteractions(listener1);
	}

	@Test
	public void testAllRegisteredListenersMatchingEventAreCalledWhenEventIsEmitted() {
		EventListener<String> listener1 = mock(EventListener.class);
		EventListener<String> listener2 = mock(EventListener.class);
		EventListener<Integer> listener3 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);
		eventEmitter.on(Integer.class, listener3);

		eventEmitter.emit(String.class, "My String");

		verify(listener1, timeout(500)).handle("My String");
		verify(listener2, timeout(500)).handle("My String");

		verifyNoMoreInteractions(listener1, listener2, listener3);
	}

	@Test
	public void testOneOffListenerOnlyCalledOnce() {
		EventListener<String> listener = mock(EventListener.class);
		EventListener<String> oneOffListener = mock(EventListener.class);

		eventEmitter.on(String.class, listener);
		eventEmitter.once(String.class, oneOffListener);

		eventEmitter.emit(String.class, "First");
		eventEmitter.emit(String.class, "Second");

		verify(listener, timeout(300)).handle(eq("First"));
		verify(listener, timeout(300)).handle(eq("Second"));
		verify(oneOffListener, timeout(300)).handle(eq("First"));

		verifyNoMoreInteractions(listener, oneOffListener);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetListenersWithNullEventThrows() {
		eventEmitter.getListeners(null);
	}

	@Test
	public void testGetListenersWithUnknwonEventReturnsEmptyList() {
		assertNotNull(eventEmitter.getListeners(String.class));
		assertTrue(eventEmitter.getListeners(String.class).isEmpty());
	}

	@Test
	public void testRegisteredListenersCanBeRetrieved() {
		EventListener<String> listener1 = mock(EventListener.class);
		EventListener<String> listener2 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		List<EventListener> listeners = eventEmitter.getListeners(String.class);
		assertNotNull(listeners);
		assertThat(listeners.size(), is(2));
		assertTrue(listeners.contains(listener1));
		assertTrue(listeners.contains(listener2));
	}

}
