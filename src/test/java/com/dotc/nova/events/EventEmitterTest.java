package com.dotc.nova.events;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.dotc.nova.events.metrics.NoopEventMetricsCollector;

@RunWith(MockitoJUnitRunner.class)
public class EventEmitterTest {
	@Mock
	private EventEmitter delegate;
	private MyEventEmitter eventEmitter;

	@Before
	public void setup() {
		eventEmitter = new MyEventEmitter(delegate);
	}

	@BeforeClass
	public static void initLogging() {
		BasicConfigurator.configure();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisteringNullEventThrows() {
		EventListener listener = mock(EventListener.class);
		eventEmitter.on(null, listener);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisteringNullListenerThrows() {
		eventEmitter.on(String.class, (EventListener) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemovingAllWithNullEventThrows() {
		eventEmitter.removeAllListeners(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemovingWithNullEventThrows() {
		EventListener listener = mock(EventListener.class);
		eventEmitter.removeListener(null, listener);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemovingWithNullListenerThrows() {
		eventEmitter.removeListener(String.class, null);
	}

	@Test
	public void testRemovingNotYetRegisteredListenerIsSilentlyIgnored() {
		EventListener listener = mock(EventListener.class);
		eventEmitter.removeListener(String.class, listener);
	}

	@Test
	public void testGetAllListeners() {
		EventListener listener1 = mock(EventListener.class);
		EventListener listener2 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.once(String.class, listener2);

		assertTrue(eventEmitter.getListeners("String.class").isEmpty());
		assertEquals(2, eventEmitter.getListeners(String.class).size());
		assertTrue(eventEmitter.getListeners(String.class).contains(listener1));
		assertTrue(eventEmitter.getListeners(String.class).contains(listener2));
	}

	@Test
	public void testGetAllRegisteredListeners() {
		EventListener listener1 = mock(EventListener.class);
		EventListener listener2 = mock(EventListener.class);
		EventListener listener3 = mock(EventListener.class);
		EventListener oneOffListener = mock(EventListener.class);

		eventEmitter.on("key1", listener1);
		eventEmitter.on("key2", listener2);
		eventEmitter.on("key1", listener3);
		eventEmitter.once("key3", oneOffListener);

		assertThat(eventEmitter.getAllListeners().entrySet(), hasSize(3));
		assertThat(eventEmitter.getAllListeners(), hasEntry(is("key1"), hasItems(listener1, listener3)));
		assertThat(eventEmitter.getAllListeners(), hasEntry(is("key2"), hasItem(listener2)));
		assertThat(eventEmitter.getAllListeners(), hasEntry(is("key3"), hasItem(oneOffListener)));
	}

	@Test
	public void testThatManipulationOnAllRegisteredListenersIsNotPutBackIntoEventEmitter() {
		EventListener listener1 = mock(EventListener.class);
		EventListener listener2 = mock(EventListener.class);

		eventEmitter.on("key1", listener1);
		eventEmitter.on("key2", listener2);

		assertThat(eventEmitter.getAllListeners().entrySet(), hasSize(2));
		eventEmitter.getAllListeners().put("key3", Arrays.asList(listener1));

		assertThat(eventEmitter.getAllListeners().entrySet(), hasSize(2));
		assertThat(eventEmitter.getListeners("key3"), hasSize(0));
	}

	@Test
	public void testGetAllRegisteredOneOffListeners() {
		EventListener listener = mock(EventListener.class);
		EventListener oneOffListener1 = mock(EventListener.class);
		EventListener oneOffListener2 = mock(EventListener.class);

		eventEmitter.once("key1", oneOffListener1);
		eventEmitter.once("key2", oneOffListener2);
		eventEmitter.on("key1", listener);

		assertThat(eventEmitter.getAllOneOffListeners().entrySet(), hasSize(2));
		assertThat(eventEmitter.getAllListeners(), hasEntry(is("key1"), hasItem(oneOffListener1)));
		assertThat(eventEmitter.getAllListeners(), hasEntry(is("key2"), hasItem(oneOffListener2)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAllListenersWithNullEventThrows() {
		eventEmitter.getListeners(null);
	}

	@Test
	public void testListenerCanBeRemoved() {
		EventListener listener1 = mock(EventListener.class);
		EventListener listener2 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		eventEmitter.emit(String.class, "MyEvent1");

		eventEmitter.removeListener(String.class, listener1);
		eventEmitter.emit(String.class, "MyEvent2");

		eventEmitter.removeListener(String.class, listener2);
		eventEmitter.emit(String.class, "MyEvent3");

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> listenerCaptorEvent1 = ArgumentCaptor.forClass(List.class);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> listenerCaptorEvent2 = ArgumentCaptor.forClass(List.class);
		verify(delegate).dispatchEventAndDataToListeners(listenerCaptorEvent1.capture(), eq(String.class),
				eq("MyEvent1"));
		verify(delegate).dispatchEventAndDataToListeners(listenerCaptorEvent2.capture(), eq(String.class),
				eq("MyEvent2"));
		verifyNoMoreInteractions(delegate);

		assertNotNull(listenerCaptorEvent1.getValue());
		assertTrue(listenerCaptorEvent1.getValue().size() == 2);
		assertTrue(listenerCaptorEvent1.getValue().contains(listener1));
		assertTrue(listenerCaptorEvent1.getValue().contains(listener2));
		assertNotNull(listenerCaptorEvent2.getValue());
		assertTrue(listenerCaptorEvent2.getValue().size() == 1);
		assertTrue(listenerCaptorEvent2.getValue().contains(listener2));
	}

	@Test
	public void testAllListenersCanBeRemoved() {

		EventListener listener1 = mock(EventListener.class);
		EventListener listener2 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.once(String.class, listener2);

		eventEmitter.removeAllListeners(String.class);
		eventEmitter.emit(String.class, "MyEvent1");

		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testListenersAreRemovedFromNormalAndOneOffMaps() {
		EventListener listener1 = mock(EventListener.class);

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
		EventListener listener1 = mock(EventListener.class);
		eventEmitter.on(String.class, listener1);
		eventEmitter.emit(String.class);

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> listenerCaptorEvent1 = ArgumentCaptor.forClass(List.class);
		verify(delegate).dispatchEventAndDataToListeners(listenerCaptorEvent1.capture(), eq(String.class));
		verifyNoMoreInteractions(delegate);

		assertNotNull(listenerCaptorEvent1.getValue());
		assertTrue(listenerCaptorEvent1.getValue().size() == 1);
		assertTrue(listenerCaptorEvent1.getValue().contains(listener1));
	}

	@Test
	public void testRegisteredListenerCalledEverytimeAnEventIsEmitted() {
		EventListener listener1 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);

		eventEmitter.emit(String.class, "MyEvent1");
		eventEmitter.emit(String.class, "MyEvent2");
		eventEmitter.emit(String.class, "MyEvent3", "MyEvent4");

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> listenerCaptorEvent1 = ArgumentCaptor.forClass(List.class);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> listenerCaptorEvent2 = ArgumentCaptor.forClass(List.class);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> listenerCaptorEvent3 = ArgumentCaptor.forClass(List.class);
		verify(delegate).dispatchEventAndDataToListeners(listenerCaptorEvent1.capture(), eq(String.class),
				eq("MyEvent1"));
		verify(delegate).dispatchEventAndDataToListeners(listenerCaptorEvent2.capture(), eq(String.class),
				eq("MyEvent2"));
		verify(delegate).dispatchEventAndDataToListeners(listenerCaptorEvent3.capture(), eq(String.class),
				eq("MyEvent3"), eq("MyEvent4"));
		verifyNoMoreInteractions(delegate);

		assertNotNull(listenerCaptorEvent1.getValue());
		assertTrue(listenerCaptorEvent1.getValue().size() == 1);
		assertTrue(listenerCaptorEvent1.getValue().contains(listener1));
		assertEquals(listenerCaptorEvent1.getValue(), listenerCaptorEvent2.getValue());
		assertEquals(listenerCaptorEvent1.getValue(), listenerCaptorEvent3.getValue());
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testAllRegisteredListenersMatchingEventAreCalledWhenEventIsEmitted() {
		EventListener listener1 = mock(EventListener.class);
		EventListener listener2 = mock(EventListener.class);
		EventListener listener3 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);
		eventEmitter.on(Integer.class, listener3);

		eventEmitter.emit(String.class, "My String");

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> listenerCaptorEvent1 = ArgumentCaptor.forClass(List.class);
		verify(delegate).dispatchEventAndDataToListeners(listenerCaptorEvent1.capture(), eq(String.class),
				eq("My String"));
		verifyNoMoreInteractions(delegate);

		assertNotNull(listenerCaptorEvent1.getValue());
		assertTrue(listenerCaptorEvent1.getValue().size() == 2);
		assertTrue(listenerCaptorEvent1.getValue().contains(listener1));
		assertTrue(listenerCaptorEvent1.getValue().contains(listener2));
	}

	@Test
	public void testOneOffListenerOnlyCalledOnce() {
		EventListener listener = mock(EventListener.class);
		EventListener oneOffListener = mock(EventListener.class);

		eventEmitter.on(String.class, listener);
		eventEmitter.once(String.class, oneOffListener);

		eventEmitter.emit(String.class, "First");
		eventEmitter.emit(String.class, "Second");

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> listenerCaptorEvent1 = ArgumentCaptor.forClass(List.class);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> listenerCaptorEvent2 = ArgumentCaptor.forClass(List.class);
		verify(delegate).dispatchEventAndDataToListeners(listenerCaptorEvent1.capture(), eq(String.class), eq("First"));
		verify(delegate)
				.dispatchEventAndDataToListeners(listenerCaptorEvent2.capture(), eq(String.class), eq("Second"));
		verifyNoMoreInteractions(delegate);

		assertNotNull(listenerCaptorEvent1.getValue());
		assertTrue(listenerCaptorEvent1.getValue().size() == 2);
		assertTrue(listenerCaptorEvent1.getValue().contains(listener));
		assertTrue(listenerCaptorEvent1.getValue().contains(oneOffListener));
		assertNotNull(listenerCaptorEvent2.getValue());
		assertTrue(listenerCaptorEvent2.getValue().size() == 1);
		assertTrue(listenerCaptorEvent2.getValue().contains(listener));

		verifyNoMoreInteractions(listener, oneOffListener);
	}

	private class MyEventEmitter extends EventEmitter {
		private final EventEmitter delegate;

		public MyEventEmitter(EventEmitter delegate) {
			super(false, new NoopEventMetricsCollector());
			this.delegate = delegate;
		}

		@Override
		void dispatchEventAndDataToListeners(List<EventListener> listenerList, Object event, Object... data) {
			delegate.dispatchEventAndDataToListeners(listenerList, event, data);
		}

	}
}
