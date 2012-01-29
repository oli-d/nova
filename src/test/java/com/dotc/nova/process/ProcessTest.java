package com.dotc.nova.process;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.apache.log4j.BasicConfigurator;
import org.easymock.Capture;
import org.junit.*;

import com.dotc.nova.ProcessingLoop;

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
		Capture<Runnable> callbackCapture = new Capture<Runnable>();

		processingLoop.dispatch(capture(callbackCapture));
		expectLastCall().once();

		replay(processingLoop);

		Runnable myCallBack = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

			}
		};

		process.nextTick(myCallBack);

		assertThat(callbackCapture.getValue(), is(myCallBack));
	}

}
