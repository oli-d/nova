package com.dotc.nova.events;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.easymock.Capture;
import org.junit.*;

import com.dotc.nova.ProcessingLoop;

public class EventEmitterTest {

	private EventEmitter eventEmitter;
	private ProcessingLoop processingLoop;

	@BeforeClass
	public static void initLogging() {
		BasicConfigurator.configure();
	}

	@Before
	public void setup() {
		processingLoop = createMock(ProcessingLoop.class);
		eventEmitter = new EventEmitter(processingLoop);
	}

	@After
	public void tearDown() {
		verify(processingLoop);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisteringNullTypeThrows() {
		replay(processingLoop);
		EventListener<String> listener = createMock(EventListener.class);
		eventEmitter.on(null, listener);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisteringNullListenerThrows() {
		replay(processingLoop);
		eventEmitter.on(String.class, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemovingAllWithNullTypeThrows() {
		replay(processingLoop);
		eventEmitter.removeAllListeners(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemovingWithNullTypeThrows() {
		replay(processingLoop);
		EventListener<String> listener = createMock(EventListener.class);
		eventEmitter.removeListener(null, listener);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemovingWithNullListenerThrows() {
		replay(processingLoop);
		eventEmitter.removeListener(String.class, null);
	}

	@Test
	public void testRemovingNotYetRegisteredListenerIsHandledSilently() {
		replay(processingLoop);
		EventListener<String> listener = createMock(EventListener.class);
		eventEmitter.removeListener(String.class, listener);
	}

	@Test
	public void testListenerCanBeRemoved() {
		EventListener<String> listener1 = createMock(EventListener.class);
		EventListener<String> listener2 = createMock(EventListener.class);

		Capture<List<EventListener>> captureListeners1 = new Capture<List<EventListener>>();
		Capture<List<EventListener>> captureListeners2 = new Capture<List<EventListener>>();
		processingLoop.dispatch(eq("MyEvent1"), capture(captureListeners1));
		expectLastCall().once();
		processingLoop.dispatch(eq("MyEvent2"), capture(captureListeners2));
		expectLastCall().once();
		replay(processingLoop);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		eventEmitter.emit("MyEvent1");

		List<EventListener> listeners = captureListeners1.getValue();
		assertNotNull(listeners);
		assertThat(listeners.size(), is(2));
		assertTrue(listeners.contains(listener1));
		assertTrue(listeners.contains(listener2));

		eventEmitter.removeListener(String.class, listener1);
		eventEmitter.emit("MyEvent2");

		listeners = captureListeners1.getValue();
		assertNotNull(listeners);
		assertThat(listeners.size(), is(1));
		assertTrue(listeners.contains(listener2));

		eventEmitter.removeListener(String.class, listener2);
		eventEmitter.emit("MyEvent3");
	}

	@Test
	public void testAllListenersCanBeRemoved() {
		EventListener<String> listener1 = createMock(EventListener.class);
		EventListener<String> listener2 = createMock(EventListener.class);

		replay(processingLoop);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		eventEmitter.removeAllListeners(String.class);
		eventEmitter.emit("MyEvent1");

	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetListenersWithNullTypeThrows() {
		replay(processingLoop);
		eventEmitter.getListeners(null);
	}

	@Test
	public void testGetListenersWithUnknwonEventTypeReturnsEmptyList() {
		replay(processingLoop);
		assertNotNull(eventEmitter.getListeners(String.class));
		assertTrue(eventEmitter.getListeners(String.class).isEmpty());
	}

	@Test
	public void testRegisteredListenersCanBeRetrieved() {
		EventListener<String> listener1 = createMock(EventListener.class);
		EventListener<String> listener2 = createMock(EventListener.class);

		replay(processingLoop);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		List<EventListener<String>> listeners = eventEmitter.getListeners(String.class);
		assertNotNull(listeners);
		assertThat(listeners.size(), is(2));
		assertTrue(listeners.contains(listener1));
		assertTrue(listeners.contains(listener2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmmittingNullThrows() {
		replay(processingLoop);
		eventEmitter.emit(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOneOffEmitWithNullEventThrows() {
		EventListener<String> listener1 = createMock(EventListener.class);
		EventListener<String> listener2 = createMock(EventListener.class);
		replay(processingLoop);
		eventEmitter.emit(null, listener1, listener2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOneOffEmitWithNullListenersThrows() {
		replay(processingLoop);
		eventEmitter.emit("Event", (EventListener<String>[]) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOneOffEmitWithEmptyListenersThrows() {
		replay(processingLoop);
		eventEmitter.emit("Event", new EventListener[0]);
	}

	@Test
	public void testRegisteredListenerCalledEverytimeAnEventIsEmitted() {
		EventListener<String> listener = createMock(EventListener.class);

		Capture<List<EventListener>> captureListeners1 = new Capture<List<EventListener>>();
		Capture<List<EventListener>> captureListeners2 = new Capture<List<EventListener>>();
		Capture<List<EventListener>> captureListeners3 = new Capture<List<EventListener>>();
		processingLoop.dispatch(eq("MyEvent1"), capture(captureListeners1));
		expectLastCall().once();
		processingLoop.dispatch(eq("MyEvent2"), capture(captureListeners2));
		expectLastCall().once();
		processingLoop.dispatch(eq("MyEvent3"), capture(captureListeners3));
		expectLastCall().once();
		replay(processingLoop);

		eventEmitter.on(String.class, listener);

		eventEmitter.emit("MyEvent1");
		eventEmitter.emit("MyEvent2");
		eventEmitter.emit("MyEvent3");

		List<EventListener> listeners = captureListeners1.getValue();
		assertNotNull(listeners);
		assertThat(listeners.size(), is(1));
		assertTrue(listeners.contains(listener));

		listeners = captureListeners2.getValue();
		assertNotNull(listeners);
		assertThat(listeners.size(), is(1));
		assertTrue(listeners.contains(listener));

		listeners = captureListeners3.getValue();
		assertNotNull(listeners);
		assertThat(listeners.size(), is(1));
		assertTrue(listeners.contains(listener));
	}

	@Test
	public void testAllRegisteredListenersMatchingEventTypeAreCalledWhenEventIsEmitted() {
		EventListener<String> handlerOne = createMock(EventListener.class);
		EventListener<String> handlerTwo = createMock(EventListener.class);
		EventListener<Integer> handlerThree = createMock(EventListener.class);

		Capture<List<EventListener>> captureListeners = new Capture<List<EventListener>>();
		processingLoop.dispatch(eq("MyEvent"), capture(captureListeners));
		expectLastCall().once();
		replay(processingLoop);

		eventEmitter.on(String.class, handlerOne);
		eventEmitter.on(String.class, handlerTwo);
		eventEmitter.on(Integer.class, handlerThree);

		eventEmitter.emit("MyEvent");

		List<EventListener> listeners = captureListeners.getValue();
		assertNotNull(listeners);
		assertThat(listeners.size(), is(2));
		assertTrue(listeners.contains(handlerOne));
		assertTrue(listeners.contains(handlerTwo));
	}

	@Test
	public void testOneOffListenersCalledWhenEmittingOneOff() {
		EventListener<String> handlerOne = createMock(EventListener.class);
		EventListener<String> handlerTwo = createMock(EventListener.class);
		EventListener<String> handlerThree = createMock(EventListener.class);

		Capture<EventListener> captureListener1 = new Capture<EventListener>();
		Capture<EventListener> captureListener2 = new Capture<EventListener>();
		processingLoop.dispatch(eq("MyEvent"), capture(captureListener1), capture(captureListener2));
		expectLastCall().once();
		replay(processingLoop);

		eventEmitter.on(String.class, handlerOne);

		eventEmitter.emit("MyEvent", handlerTwo, handlerThree);

		// maven CLI issues when using assertThat(..., is(...)) !?!?!?
		EventListener listener1 = captureListener1.getValue();
		assertTrue(listener1 == handlerTwo);
		EventListener listener2 = captureListener2.getValue();
		assertTrue(listener2 == handlerThree);
	}

}
