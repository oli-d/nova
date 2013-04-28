package com.dotc.nova.filesystem;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.file.NoSuchFileException;

import org.easymock.Capture;
import org.junit.*;

import com.dotc.nova.TestHelper;

public class FilesystemTest {
	private Filesystem filesystem;
	private com.dotc.nova.process.Process process;

	@Before
	public void setup() {
		process = createStrictMock(com.dotc.nova.process.Process.class);
		filesystem = new Filesystem(process);
	}

	@After
	public void tearDown() {
		verify(process);
	}

	@Test
	public void testReadFile() throws Throwable {
		FileReadHandler handler = createMock(FileReadHandler.class);
		handler.fileRead(eq("This is some content in some file."));
		expectLastCall().once();

		Capture<Runnable> runnableCapture = new Capture<>();
		process.nextTick(capture(runnableCapture));
		expectLastCall().once();

		replay(process, handler);

		filesystem.readFile("src/test/resources/someFile.txt", handler);

		Runnable dispatchedRunnable = TestHelper.getCaptureValue(runnableCapture);
		assertNotNull(dispatchedRunnable);
		dispatchedRunnable.run();

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

		replay(process, handler);

		filesystem.readFile("doesntExist.txt", handler);
		// Java6: TestHelper.getCaptureValue(runnableCapture).run();
		verify(handler);
	}

	@Test
	public void testReadFileSync() throws Throwable {
		replay(process);
		assertThat(filesystem.readFileSync("src/test/resources/someFile.txt"), is("This is some content in some file."));
	}

}
