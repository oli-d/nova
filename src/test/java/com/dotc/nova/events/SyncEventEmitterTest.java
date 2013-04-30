package com.dotc.nova.events;

import org.junit.Before;

public class SyncEventEmitterTest extends EventEmitterTestBase {
	@Before
	public void setup() {
		super.eventEmitter = new SyncEventEmitter();
	}

	/*
	 * @Override
	 * 
	 * @Test(expected = IllegalArgumentException.class) public void testRegisteringNullTypeThrows() { EventListener<String> listener = createMock(EventListener.class); eventEmitter.on(null, listener);
	 * }
	 * 
	 * @Override
	 * 
	 * @Test(expected = IllegalArgumentException.class) public void testRegisteringNullListenerThrows() { eventEmitter.on(String.class, null); }
	 * 
	 * @Override
	 * 
	 * @Test(expected = IllegalArgumentException.class) public void testRemovingAllWithNullTypeThrows() { eventEmitter.removeAllListeners(null); }
	 * 
	 * @Override
	 * 
	 * @Test(expected = IllegalArgumentException.class) public void testRemovingWithNullTypeThrows() { EventListener<String> listener = createMock(EventListener.class);
	 * eventEmitter.removeListener(null, listener); }
	 * 
	 * @Override
	 * 
	 * @Test(expected = IllegalArgumentException.class) public void testRemovingWithNullListenerThrows() { eventEmitter.removeListener(String.class, null); }
	 * 
	 * @Test public void testRemovingNotYetRegisteredListenerIsHandledSilently() { EventListener<String> listener = createMock(EventListener.class); eventEmitter.removeListener(String.class,
	 * listener); }
	 * 
	 * @Override
	 * 
	 * @Test public void testListenerCanBeRemoved() { EventListener<String> listener1 = createMock(EventListener.class); EventListener<String> listener2 = createMock(EventListener.class);
	 * 
	 * Capture<List<EventListener>> captureListeners1 = new Capture<List<EventListener>>(); Capture<List<EventListener>> captureListeners2 = new Capture<List<EventListener>>();
	 * processingLoop.dispatch(eq(String.class), capture(captureListeners1), eq("MyEvent1")); expectLastCall().once(); processingLoop.dispatch(eq(String.class), capture(captureListeners2),
	 * eq("MyEvent2")); expectLastCall().once(); replay(processingLoop);
	 * 
	 * eventEmitter.on(String.class, listener1); eventEmitter.on(String.class, listener2);
	 * 
	 * eventEmitter.emit(String.class, "MyEvent1");
	 * 
	 * List<EventListener> listeners = captureListeners1.getValue(); assertNotNull(listeners); assertThat(listeners.size(), is(2)); assertTrue(listeners.contains(listener1));
	 * assertTrue(listeners.contains(listener2));
	 * 
	 * eventEmitter.removeListener(String.class, listener1); eventEmitter.emit(String.class, "MyEvent2");
	 * 
	 * listeners = captureListeners2.getValue(); assertNotNull(listeners); assertThat(listeners.size(), is(1)); assertTrue(listeners.contains(listener2));
	 * 
	 * eventEmitter.removeListener(String.class, listener2); eventEmitter.emit(String.class, "MyEvent3"); }
	 * 
	 * @Override
	 * 
	 * @Test public void testAllListenersCanBeRemoved() { EventListener<String> listener1 = createMock(EventListener.class); EventListener<String> listener2 = createMock(EventListener.class);
	 * 
	 * eventEmitter.on(String.class, listener1); eventEmitter.on(String.class, listener2);
	 * 
	 * eventEmitter.removeAllListeners(String.class); eventEmitter.emit("MyEvent1");
	 * 
	 * }
	 * 
	 * @Override
	 * 
	 * @Test public void testListenersAreRemovedFromNormalAndOneOffMaps() { EventListener<String> listener1 = createMock(EventListener.class);
	 * 
	 * eventEmitter.on(String.class, listener1); eventEmitter.once(String.class, listener1);
	 * 
	 * eventEmitter.removeListener(String.class, listener1); eventEmitter.emit("MyEvent1");
	 * 
	 * assertThat(eventEmitter.getHandlers(String.class).size(), is(0));
	 * 
	 * }
	 * 
	 * @Override
	 * 
	 * @Test(expected = IllegalArgumentException.class) public void testGetListenersWithNullTypeThrows() { eventEmitter.getHandlers(null); }
	 * 
	 * @Override
	 * 
	 * @Test public void testGetListenersWithUnknwonEventTypeReturnsEmptyList() { assertNotNull(eventEmitter.getHandlers(String.class)); assertTrue(eventEmitter.getHandlers(String.class).isEmpty()); }
	 * 
	 * @Override
	 * 
	 * @Test public void testRegisteredListenersCanBeRetrieved() { EventListener<String> listener1 = createMock(EventListener.class); EventListener<String> listener2 = createMock(EventListener.class);
	 * 
	 * eventEmitter.on(String.class, listener1); eventEmitter.on(String.class, listener2);
	 * 
	 * List<EventListener> listeners = eventEmitter.getHandlers(String.class); assertNotNull(listeners); assertThat(listeners.size(), is(2)); assertTrue(listeners.contains(listener1));
	 * assertTrue(listeners.contains(listener2)); }
	 * 
	 * @Override
	 * 
	 * @Test(expected = IllegalArgumentException.class) public void testEmmittingNullThrows() { eventEmitter.emit(null); }
	 * 
	 * @Override
	 * 
	 * @Test(expected = IllegalArgumentException.class) public void testOneOffEmitWithNullEventThrows() { EventListener<String> listener1 = createMock(EventListener.class); EventListener<String>
	 * listener2 = createMock(EventListener.class); eventEmitter.emit(null, listener1, listener2); }
	 * 
	 * @Override
	 * 
	 * @Test public void testRegisteredListenerCalledEverytimeAnEventIsEmitted() { EventListener<String> listener = createMock(EventListener.class);
	 * 
	 * Capture<List<EventListener>> captureListeners1 = new Capture<List<EventListener>>(); Capture<List<EventListener>> captureListeners2 = new Capture<List<EventListener>>();
	 * Capture<List<EventListener>> captureListeners3 = new Capture<List<EventListener>>(); processingLoop.dispatch(eq(String.class), capture(captureListeners1), eq("MyEvent1"));
	 * expectLastCall().once(); processingLoop.dispatch(eq(String.class), capture(captureListeners2), eq("MyEvent2")); expectLastCall().once(); processingLoop.dispatch(eq(String.class),
	 * capture(captureListeners3), eq("MyEvent3"), eq("MyEvent4")); expectLastCall().once(); replay(processingLoop);
	 * 
	 * eventEmitter.on(String.class, listener);
	 * 
	 * eventEmitter.emit(String.class, "MyEvent1"); eventEmitter.emit(String.class, "MyEvent2"); eventEmitter.emit(String.class, "MyEvent3", "MyEvent4");
	 * 
	 * List<EventListener> listeners = captureListeners1.getValue(); assertNotNull(listeners); assertThat(listeners.size(), is(1)); assertTrue(listeners.contains(listener));
	 * 
	 * listeners = captureListeners2.getValue(); assertNotNull(listeners); assertThat(listeners.size(), is(1)); assertTrue(listeners.contains(listener));
	 * 
	 * listeners = captureListeners3.getValue(); assertNotNull(listeners); assertThat(listeners.size(), is(1)); assertTrue(listeners.contains(listener)); }
	 * 
	 * @Override
	 * 
	 * @Test public void testAllRegisteredListenersMatchingEventAreCalledWhenEventIsEmitted() { EventListener<String> handlerOne = createMock(EventListener.class); EventListener<String> handlerTwo =
	 * createMock(EventListener.class); EventListener<Integer> handlerThree = createMock(EventListener.class);
	 * 
	 * Capture<List<EventListener>> captureListeners = new Capture<List<EventListener>>(); processingLoop.dispatch(eq(String.class), capture(captureListeners), eq("MyEvent")); expectLastCall().once();
	 * replay(processingLoop);
	 * 
	 * eventEmitter.on(String.class, handlerOne); eventEmitter.on(String.class, handlerTwo); eventEmitter.on(Integer.class, handlerThree);
	 * 
	 * eventEmitter.emit(String.class, "MyEvent");
	 * 
	 * List<EventListener> listeners = captureListeners.getValue(); assertNotNull(listeners); assertThat(listeners.size(), is(2)); assertTrue(listeners.contains(handlerOne));
	 * assertTrue(listeners.contains(handlerTwo)); }
	 * 
	 * @Override
	 * 
	 * @Test public void testOneOffListenerOnlyCalledOnce() { EventListener<String> listener = createMock(EventListener.class); EventListener<String> oneOffListener = createMock(EventListener.class);
	 * 
	 * Capture<List<EventListener>> captureListeners1 = new Capture<List<EventListener>>(); processingLoop.dispatch(eq(String.class), capture(captureListeners1), eq("First")); expectLastCall().once();
	 * Capture<List<EventListener>> captureListeners2 = new Capture<List<EventListener>>(); processingLoop.dispatch(eq(String.class), capture(captureListeners2), eq("Second"));
	 * expectLastCall().once(); replay(processingLoop);
	 * 
	 * eventEmitter.on(String.class, listener); eventEmitter.once(String.class, oneOffListener);
	 * 
	 * eventEmitter.emit(String.class, "First"); eventEmitter.emit(String.class, "Second");
	 * 
	 * List<EventListener> listeners1 = captureListeners1.getValue(); assertNotNull(listeners1); assertThat(listeners1.size(), is(2)); assertTrue(listeners1.contains(listener));
	 * assertTrue(listeners1.contains(oneOffListener));
	 * 
	 * List<EventListener> listeners2 = captureListeners2.getValue(); assertNotNull(listeners2); assertThat(listeners2.size(), is(1)); assertTrue(listeners2.contains(listener)); }
	 */
}
