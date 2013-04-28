package com.dotc.nova.filesystem;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.file.NoSuchFileException;

import org.easymock.Capture;
import org.junit.*;

import com.dotc.nova.ProcessingLoop;

public class FilesystemTest {
	private Filesystem filesystem;
	private ProcessingLoop processingLoop;

	@Before
	public void setup() {
		processingLoop = createStrictMock(ProcessingLoop.class);
		filesystem = new Filesystem(processingLoop);
	}

	@After
	public void tearDown() {
		verify(processingLoop);
	}

	@Test
	public void testReadFile() throws Throwable {
		FileReadHandler handler = createMock(FileReadHandler.class);
		handler.fileRead(eq("This is some content in some file."));
		expectLastCall().once();

		Capture<Runnable> runnableCapture = new Capture<Runnable>();
		processingLoop.dispatch(capture(runnableCapture));
		expectLastCall().once();

		replay(processingLoop, handler);

		filesystem.readFile("src/test/resources/someFile.txt", handler);
		long maxTime = System.currentTimeMillis() + 10000l;
		Runnable runnable = null;
		while (runnable == null && System.currentTimeMillis() <= maxTime) {
			Thread.sleep(25);
			try {
				runnable = runnableCapture.getValue();
			} catch (Exception e) {
				// noop
			}
		}
		assertNotNull(runnable);

		runnable.run();

		verify(handler);
	}

	@Test
	public void testReadFileAsyncWithUnknownPathCausesErrorCallbackToBeInvoked() throws Throwable {
		FileReadHandler handler = createMock(FileReadHandler.class);
		handler.errorOccurred(anyObject(NoSuchFileException.class));
		expectLastCall().once();

		/*
		 * Java6 Capture<Runnable> runnableCapture = new Capture<Runnable>(); processingLoop.dispatch(capture(runnableCapture));
		 */

		replay(processingLoop, handler);

		filesystem.readFile("doesntExist.txt", handler);
		// Java6: TestHelper.getCaptureValue(runnableCapture).run();
		verify(handler);
	}

	@Test
	public void testReadFileSync() throws Throwable {
		replay(processingLoop);
		assertThat(filesystem.readFileSync("src/test/resources/someFile.txt"), is("This is some content in some file."));
	}

}
