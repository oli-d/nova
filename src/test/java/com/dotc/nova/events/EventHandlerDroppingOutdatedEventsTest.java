package com.dotc.nova.events;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventHandlerDroppingOutdatedEventsTest {
	private EventHandlerDroppingOutdatedEvents sut;

	@Before
	public void setup() {
		sut = new EventHandlerDroppingOutdatedEvents();
	}

	@Test
	public void testEventIsImediatelyDispatchedIfNotInBatchMode() throws Exception {
		EventListener<String> listener = Mockito.mock(EventListener.class);

		InvocationContext ic = new InvocationContext("1", listener, "data");
		sut.onEvent(ic, 1, true);

		Mockito.verify(listener).handle("data");
		Mockito.verifyNoMoreInteractions(listener);
	}

	@Test
	public void testDifferentEventsAreQueuedAndProcessedInOneGoIfBatchModeEnds() throws Exception {
		EventListener<String> listener = Mockito.mock(EventListener.class);

		InvocationContext ic1 = new InvocationContext("1", listener, "data1");
		InvocationContext ic2 = new InvocationContext("2", listener, "data2");
		sut.onEvent(ic1, 1, false);

		// verify, that after the first event we didn't do anything
		Mockito.verifyNoMoreInteractions(listener);

		// send second event, which ends the batch mode
		sut.onEvent(ic2, 2, true);

		// verify that all listeners were called
		Mockito.verify(listener).handle("data1");
		Mockito.verify(listener).handle("data2");
		Mockito.verifyNoMoreInteractions(listener);
	}

	@Test
	public void testSameEventsOverrideEachOtherInBatchMode() throws Exception {
		EventListener<String> listener = Mockito.mock(EventListener.class);

		InvocationContext ic1 = new InvocationContext("1", listener, "data1");
		InvocationContext ic2 = new InvocationContext("1", listener, "data2");
		sut.onEvent(ic1, 1, false);

		// verify, that after the first event we didn't do anything
		Mockito.verifyNoMoreInteractions(listener);

		// send second event, which ends the batch mode
		sut.onEvent(ic2, 2, true);

		// verify that all listeners were called
		Mockito.verify(listener).handle("data2");
		Mockito.verifyNoMoreInteractions(listener);
	}

	@Test
	public void testOrderIsKeptIfIgnorableEventsOverrideEachOtherInBatchMode() throws Exception {
		EventListener<String> listener = Mockito.mock(EventListener.class);

		sut.onEvent(new InvocationContext("1", listener, "data1a"), 1, false);
		sut.onEvent(new InvocationContext("1", listener, "data1b"), 2, false);
		sut.onEvent(new InvocationContext("2", listener, "data2a"), 3, false);
		sut.onEvent(new InvocationContext("2", listener, "data2b"), 4, false);
		sut.onEvent(new InvocationContext("3", listener, "data3a"), 5, false);
		sut.onEvent(new InvocationContext("2", listener, "data2c"), 6, false);
		sut.onEvent(new InvocationContext("2", listener, "data2d"), 7, false);
		sut.onEvent(new InvocationContext("1", listener, "data1c"), 8, false);
		sut.onEvent(new InvocationContext("4", listener, "data4a"), 9, false);
		sut.onEvent(new InvocationContext("4", listener, "data4b"), 10, false);
		sut.onEvent(new InvocationContext("2", listener, "data2e"), 11, false);
		sut.onEvent(new InvocationContext("2", listener, "data2f"), 12, false);
		sut.onEvent(new InvocationContext("4", listener, "data4c"), 13, false);

		// verify, that we didn't do anything so far
		Mockito.verifyNoMoreInteractions(listener);

		sut.onEvent(new InvocationContext("2", listener, "data2g"), 14, true);

		InOrder inOrder = Mockito.inOrder(listener);
		inOrder.verify(listener).handle("data1c");
		inOrder.verify(listener).handle("data2g");
		inOrder.verify(listener).handle("data3a");
		inOrder.verify(listener).handle("data4c");
		Mockito.verifyNoMoreInteractions(listener);
	}

	@Test
	public void testAllListenersAreCalledIfIgnorableEventsOverrideEachOtherInBatchMode() throws Exception {
		EventListener<String> listener1 = Mockito.mock(EventListener.class);
		EventListener<String> listener2 = Mockito.mock(EventListener.class);
		EventListener<String> listener3 = Mockito.mock(EventListener.class);

		sut.onEvent(new InvocationContext("1", listener1, "data1a"), 1, false);
		sut.onEvent(new InvocationContext("1", listener1, "data1b"), 2, false);
		sut.onEvent(new InvocationContext("1", listener2, "data1c"), 3, false);
		sut.onEvent(new InvocationContext("1", listener2, "data1d"), 4, false);
		sut.onEvent(new InvocationContext("1", listener3, "data1e"), 5, false);

		// verify, that we didn't do anything so far
		Mockito.verifyNoMoreInteractions(listener1, listener2, listener3);

		sut.onEvent(new InvocationContext("1", listener3, "data1f"), 14, true);

		Mockito.verify(listener1).handle("data1f");
		Mockito.verify(listener2).handle("data1f");
		Mockito.verify(listener3).handle("data1f");

		Mockito.verifyNoMoreInteractions(listener1, listener2, listener3);
	}

}
