package com.dotc.nova.events;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.easymock.Capture;
import org.junit.*;

import com.dotc.nova.ProcessingLoop;
import com.dotc.nova.TestHelper;

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
		EventListener<String> listener = createMock(EventListener.class);
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
		EventListener<String> listener = createMock(EventListener.class);
		eventEmitter.removeListener(null, listener);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemovingWithNullListenerThrows() {
		eventEmitter.removeListener(String.class, null);
	}

	@Test
	public void testRemovingNotYetRegisteredListenerIsSilentlyIgnored() {
		EventListener<String> listener = createMock(EventListener.class);
		eventEmitter.removeListener(String.class, listener);
	}

	@Test
	public void testListenerCanBeRemoved() {
		EventListener<String> listener1 = createMock(EventListener.class);
		EventListener<String> listener2 = createMock(EventListener.class);

		listener1.handle(eq("MyEvent1"));
		expectLastCall().once();

		listener2.handle(eq("MyEvent1"));
		expectLastCall().once();
		Capture<String> paramCapture = new Capture<>();
		listener2.handle(capture(paramCapture));
		expectLastCall().once();

		replay(listener1, listener2);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		eventEmitter.emit(String.class, "MyEvent1");

		eventEmitter.removeListener(String.class, listener1);
		eventEmitter.emit(String.class, "MyEvent2");

		eventEmitter.removeListener(String.class, listener2);
		eventEmitter.emit(String.class, "MyEvent3");

		String paramValue = TestHelper.getCaptureValue(paramCapture);
		assertThat(paramValue, is("MyEvent2"));

		verify(listener1, listener2);
	}

	@Test
	public void testAllListenersCanBeRemoved() {
		EventListener<String> listener1 = createMock(EventListener.class);
		EventListener<String> listener2 = createMock(EventListener.class);

		replay(listener1, listener2);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		eventEmitter.removeAllListeners(String.class);
		eventEmitter.emit(String.class, "MyEvent1");

		// ugly, but just to be sure that nothing is invoked, we give it a little time:
		try {
			Thread.sleep(1000l);
		} catch (InterruptedException e) {
		}

		verify(listener1, listener2);
	}

	@Test
	public void testListenersAreRemovedFromNormalAndOneOffMaps() {
		EventListener<String> listener1 = createMock(EventListener.class);
		replay(listener1);

		eventEmitter.on(String.class, listener1);
		eventEmitter.once(String.class, listener1);

		eventEmitter.removeListener(String.class, listener1);
		eventEmitter.emit("MyEvent1");

		// ugly, but just to be sure that nothing is invoked, we give it a little time:
		try {
			Thread.sleep(1000l);
		} catch (InterruptedException e) {
		}

		verify(listener1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmmittingNullThrows() {
		eventEmitter.emit(null);
	}

	@Test
	public void testRegisteredListenerIsCalledWithoutDataBeingPassed() {
		EventListener<String> listener1 = createMock(EventListener.class);

		listener1.handle((String[]) null);
		expectLastCall().once();

		replay(listener1);

		eventEmitter.on(String.class, listener1);

		eventEmitter.emit(String.class);

		// ugly, but we give it a little time:
		try {
			Thread.sleep(1000l);
		} catch (InterruptedException e) {
		}

		verify(listener1);
	}

	@Test
	public void testRegisteredListenerCalledEverytimeAnEventIsEmitted() {
		EventListener<String> listener1 = createMock(EventListener.class);

		Capture<String> captureParam1 = new Capture<String>();
		Capture<String> captureParam2 = new Capture<String>();
		Capture<String> captureParam3 = new Capture<String>();
		Capture<String> captureParam4 = new Capture<String>();

		listener1.handle(capture(captureParam1));
		expectLastCall().once();
		listener1.handle(capture(captureParam2));
		expectLastCall().once();
		listener1.handle(capture(captureParam3), capture(captureParam4));
		expectLastCall().once();

		replay(listener1);

		eventEmitter.on(String.class, listener1);

		eventEmitter.emit(String.class, "MyEvent1");
		eventEmitter.emit(String.class, "MyEvent2");
		eventEmitter.emit(String.class, "MyEvent3", "MyEvent4");

		String param4 = TestHelper.getCaptureValue(captureParam4);
		assertNotNull(param4);
		assertThat(param4, is("MyEvent4"));
		String param1 = captureParam1.getValue();
		assertThat(param1, is("MyEvent1"));
		String param2 = captureParam2.getValue();
		assertThat(param2, is("MyEvent2"));
		String param3 = captureParam3.getValue();
		assertThat(param3, is("MyEvent3"));

		verify(listener1);
	}

	@Test
	public void testAllRegisteredListenersMatchingEventAreCalledWhenEventIsEmitted() {
		EventListener<String> listener1 = createMock(EventListener.class);
		EventListener<String> listener2 = createMock(EventListener.class);
		EventListener<Integer> listener3 = createMock(EventListener.class);

		Capture<String> captureListener1 = new Capture<String>();
		listener1.handle(capture(captureListener1));
		expectLastCall().once();
		Capture<String> captureListener2 = new Capture<String>();
		listener2.handle(capture(captureListener2));
		expectLastCall().once();

		replay(listener1, listener2, listener3);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);
		eventEmitter.on(Integer.class, listener3);

		eventEmitter.emit(String.class, "My String");

		String paramListener1 = TestHelper.getCaptureValue(captureListener1);
		assertNotNull(paramListener1);
		assertThat(paramListener1, is("My String"));
		String paramListener2 = TestHelper.getCaptureValue(captureListener2);
		assertNotNull(paramListener2);
		assertThat(paramListener2, is("My String"));

		verify(listener1, listener2, listener3);
	}

	@Test
	public void testOneOffListenerOnlyCalledOnce() {
		EventListener<String> listener = createMock(EventListener.class);
		EventListener<String> oneOffListener = createMock(EventListener.class);

		listener.handle(eq("First"));
		expectLastCall().once();
		Capture<String> captureParam = new Capture<String>();
		listener.handle(capture(captureParam));
		expectLastCall().once();
		oneOffListener.handle(eq("First"));
		expectLastCall().once();

		replay(listener, oneOffListener);

		eventEmitter.on(String.class, listener);
		eventEmitter.once(String.class, oneOffListener);

		eventEmitter.emit(String.class, "First");
		eventEmitter.emit(String.class, "Second");

		String param = TestHelper.getCaptureValue(captureParam);
		assertNotNull(param);
		assertThat(param, is("Second"));

		verify(listener, oneOffListener);
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
		EventListener<String> listener1 = createMock(EventListener.class);
		EventListener<String> listener2 = createMock(EventListener.class);

		eventEmitter.on(String.class, listener1);
		eventEmitter.on(String.class, listener2);

		List<EventListener> listeners = eventEmitter.getListeners(String.class);
		assertNotNull(listeners);
		assertThat(listeners.size(), is(2));
		assertTrue(listeners.contains(listener1));
		assertTrue(listeners.contains(listener2));
	}

}
