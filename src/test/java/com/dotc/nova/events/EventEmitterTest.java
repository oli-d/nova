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
		EventHandler<String> listener = createMock(EventHandler.class);
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
		EventHandler<String> listener = createMock(EventHandler.class);
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
		EventHandler<String> listener = createMock(EventHandler.class);
		eventEmitter.removeListener(String.class, listener);
	}

	@Test
	public void testListenerCanBeRemoved() {
		EventHandler<String> listener1 = createMock(EventHandler.class);
		EventHandler<String> listener2 = createMock(EventHandler.class);

		Capture<List<EventHandler>> captureListeners1 = new Capture<List<EventHandler>>();
		Capture<List<EventHandler>> captureListeners2 = new Capture<List<EventHandler>>();
		processingLoop.dispatch(eq(String.class), capture(captureListeners1), eq("MyEvent1"));
		expectLastCall().once();
		processingLoop.dispatch(eq(String.class), capture(captureListeners2), eq("MyEvent2"));
		expectLastCall().once();
		replay(processingLoop);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		eventEmitter.emit(String.class, "MyEvent1");

		List<EventHandler> listeners = captureListeners1.getValue();
		assertNotNull(listeners);
		assertThat(listeners.size(), is(2));
		assertTrue(listeners.contains(listener1));
		assertTrue(listeners.contains(listener2));

		eventEmitter.removeListener(String.class, listener1);
		eventEmitter.emit(String.class, "MyEvent2");

		listeners = captureListeners1.getValue();
		assertNotNull(listeners);
		assertThat(listeners.size(), is(1));
		assertTrue(listeners.contains(listener2));

		eventEmitter.removeListener(String.class, listener2);
		eventEmitter.emit(String.class, "MyEvent3");
	}

	@Test
	public void testAllListenersCanBeRemoved() {
		EventHandler<String> listener1 = createMock(EventHandler.class);
		EventHandler<String> listener2 = createMock(EventHandler.class);

		replay(processingLoop);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		eventEmitter.removeAllListeners(String.class);
		eventEmitter.emit("MyEvent1");

	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetListenersWithNullTypeThrows() {
		replay(processingLoop);
		eventEmitter.getHandlers(null);
	}

	@Test
	public void testGetListenersWithUnknwonEventTypeReturnsEmptyList() {
		replay(processingLoop);
		assertNotNull(eventEmitter.getHandlers(String.class));
		assertTrue(eventEmitter.getHandlers(String.class).isEmpty());
	}

	@Test
	public void testRegisteredListenersCanBeRetrieved() {
		EventHandler<String> listener1 = createMock(EventHandler.class);
		EventHandler<String> listener2 = createMock(EventHandler.class);

		replay(processingLoop);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		List<EventHandler> listeners = eventEmitter.getHandlers(String.class);
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
		EventHandler<String> listener1 = createMock(EventHandler.class);
		EventHandler<String> listener2 = createMock(EventHandler.class);
		replay(processingLoop);
		eventEmitter.emit(null, listener1, listener2);
	}

	@Test
	public void testRegisteredListenerCalledEverytimeAnEventIsEmitted() {
		EventHandler<String> listener = createMock(EventHandler.class);

		Capture<List<EventHandler>> captureListeners1 = new Capture<List<EventHandler>>();
		Capture<List<EventHandler>> captureListeners2 = new Capture<List<EventHandler>>();
		Capture<List<EventHandler>> captureListeners3 = new Capture<List<EventHandler>>();
		processingLoop.dispatch(eq(String.class), capture(captureListeners1), eq("MyEvent1"));
		expectLastCall().once();
		processingLoop.dispatch(eq(String.class), capture(captureListeners2), eq("MyEvent2"));
		expectLastCall().once();
		processingLoop.dispatch(eq(String.class), capture(captureListeners3), eq("MyEvent3"), eq("MyEvent4"));
		expectLastCall().once();
		replay(processingLoop);

		eventEmitter.on(String.class, listener);

		eventEmitter.emit(String.class, "MyEvent1");
		eventEmitter.emit(String.class, "MyEvent2");
		eventEmitter.emit(String.class, "MyEvent3", "MyEvent4");

		List<EventHandler> listeners = captureListeners1.getValue();
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
	public void testAllRegisteredListenersMatchingEventAreCalledWhenEventIsEmitted() {
		EventHandler<String> handlerOne = createMock(EventHandler.class);
		EventHandler<String> handlerTwo = createMock(EventHandler.class);
		EventHandler<Integer> handlerThree = createMock(EventHandler.class);

		Capture<List<EventHandler>> captureListeners = new Capture<List<EventHandler>>();
		processingLoop.dispatch(eq(String.class), capture(captureListeners), eq("MyEvent"));
		expectLastCall().once();
		replay(processingLoop);

		eventEmitter.on(String.class, handlerOne);
		eventEmitter.on(String.class, handlerTwo);
		eventEmitter.on(Integer.class, handlerThree);

		eventEmitter.emit(String.class, "MyEvent");

		List<EventHandler> listeners = captureListeners.getValue();
		assertNotNull(listeners);
		assertThat(listeners.size(), is(2));
		assertTrue(listeners.contains(handlerOne));
		assertTrue(listeners.contains(handlerTwo));
	}

}
