package com.dotc.nova.process;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.log4j.BasicConfigurator;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.dotc.nova.ProcessingLoop;
import com.dotc.nova.events.EventListener;

@RunWith(MockitoJUnitRunner.class)
public class ProcessTest {

	private Process process;
	@Mock
	private ProcessingLoop processingLoop;

	@BeforeClass
	public static void initLogging() {
		BasicConfigurator.configure();
	}

	@Before
	public void setup() {
		process = new Process(processingLoop);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNextTickPassingNullThrows() {
		process.nextTick(null);
	}

	@Test
	public void testNextTickPutsCallbackOnProcessingLoop() {
		Runnable myCallback = mock(Runnable.class);

		process.nextTick(myCallback);

		// wait for listener to be generated and put on the event loop, and invoke it
		ArgumentCaptor<EventListener> listenerCaptor = ArgumentCaptor.forClass(EventListener.class);
		verify(processingLoop, timeout(500)).dispatch(listenerCaptor.capture());
		assertNotNull(listenerCaptor.getValue());
		listenerCaptor.getValue().handle();

		verify(myCallback, times(1)).run();
	}

}
