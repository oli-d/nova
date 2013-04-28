package com.dotc.nova.process;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.apache.log4j.BasicConfigurator;
import org.easymock.Capture;
import org.junit.*;

import com.dotc.nova.ProcessingLoop;
import com.dotc.nova.TestHelper;
import com.dotc.nova.events.EventListener;

public class ProcessTest {

	private Process process;
	private ProcessingLoop processingLoop;

	@BeforeClass
	public static void initLogging() {
		BasicConfigurator.configure();
	}

	@Before
	public void setup() {
		processingLoop = createMock(ProcessingLoop.class);
		process = new Process(processingLoop);
	}

	@After
	public void tearDown() {
		verify(processingLoop);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNextTickPassingNullThrows() {
		replay(processingLoop);
		process.nextTick(null);
	}

	@Test
	public void testNextTickPutsCallbackOnProcessingLoop() {
		Runnable myCallback = createMock(Runnable.class);
		myCallback.run();
		expectLastCall().once();

		Capture<EventListener> listenerCaprure = new Capture<>();
		processingLoop.dispatch(capture(listenerCaprure));
		expectLastCall().once();

		replay(processingLoop, myCallback);

		process.nextTick(myCallback);

		// wait for listener to be generated and put on the event loop, and invoke it
		EventListener captureValue = TestHelper.getCaptureValue(listenerCaprure);
		assertNotNull(captureValue);
		captureValue.handleEventWithData();

		verify(processingLoop, myCallback);
	}

}
