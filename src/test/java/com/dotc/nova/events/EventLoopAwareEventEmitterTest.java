package com.dotc.nova.events;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.dotc.nova.events.metrics.NoopEventMetricsCollector;

@RunWith(MockitoJUnitRunner.class)
public class EventLoopAwareEventEmitterTest {
	@Mock
	private EventLoop eventLoop;
	private EventLoopAwareEventEmitter eventEmitter;

	@Before
	public void setup() {
		eventEmitter = new EventLoopAwareEventEmitter(eventLoop, false, new NoopEventMetricsCollector());
	}

	@Test
	public void testListenerInvocationDelegatedToEventLoop() {
		EventListener<String> listener1 = mock(EventListener.class);
		EventListener<String> listener2 = mock(EventListener.class);
		EventListener<String> listener3 = mock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);
		eventEmitter.on(String.class, listener3);

		verifyNoMoreInteractions(eventLoop);

		eventEmitter.emit(String.class, "First");
		eventEmitter.emit(String.class, "Second");

		ArgumentCaptor<List> listenerCaptorEvent1 = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<List> listenerCaptorEvent2 = ArgumentCaptor.forClass(List.class);
		verify(eventLoop).dispatch(eq(String.class), listenerCaptorEvent1.capture(), eq("First"));
		verify(eventLoop).dispatch(eq(String.class), listenerCaptorEvent2.capture(), eq("Second"));
		verifyNoMoreInteractions(eventLoop);

		assertNotNull(listenerCaptorEvent1.getValue());
		assertTrue(listenerCaptorEvent1.getValue().size() == 3);
		assertTrue(listenerCaptorEvent1.getValue().contains(listener1));
		assertTrue(listenerCaptorEvent1.getValue().contains(listener2));
		assertTrue(listenerCaptorEvent1.getValue().contains(listener3));
		assertEquals(listenerCaptorEvent1.getValue(), listenerCaptorEvent2.getValue());
	}

}
