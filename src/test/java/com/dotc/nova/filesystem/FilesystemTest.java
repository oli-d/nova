package com.dotc.nova.filesystem;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.NoSuchFileException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.dotc.nova.TestHelper;

@RunWith(MockitoJUnitRunner.class)
public class FilesystemTest {
	private Filesystem filesystem;
	@Mock
	private com.dotc.nova.process.Process process;

	@Before
	public void setup() {
		filesystem = new Filesystem(process);
	}

	@Test
	public void testReadFile() throws Throwable {
		FileReadHandler handler = mock(FileReadHandler.class);

		filesystem.readFile("src/test/resources/someFile.txt", handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		Thread.sleep(500); // FIXME: how can we get rid off this sleep?
		verify(process).nextTick(captor.capture());

		Runnable dispatchedRunnable = TestHelper.getCaptorValue(captor);
		assertNotNull(dispatchedRunnable);
		dispatchedRunnable.run();

		verify(handler).fileRead(eq("This is some content in some file."));

		verifyNoMoreInteractions(handler, process);
	}

	@Test
	public void testReadFileAsyncWithUnknownPathCausesErrorCallbackToBeInvoked() throws Throwable {
		FileReadHandler handler = mock(FileReadHandler.class);

		/*
		 * Java6 Capture<Runnable> runnableCapture = new Capture<Runnable>(); processingLoop.dispatch(capture(runnableCapture));
		 */

		filesystem.readFile("doesntExist.txt", handler);
		// Java6: TestHelper.getCaptureValue(runnableCapture).run();
		verify(handler).errorOccurred((Throwable) anyObject());

		verifyNoMoreInteractions(handler, process);
	}

	@Test
	public void testReadFileSync() throws Throwable {
		assertThat(filesystem.readFileSync("src/test/resources/someFile.txt"), is("This is some content in some file."));
		verifyZeroInteractions(process);
	}

	@Test(expected = NoSuchFileException.class)
	public void testReadFileSyncWithUnknownPathThrowsException() throws Throwable {
		filesystem.readFileSync("isntThere.txt");
		verifyZeroInteractions(process);
	}

	@Test
	public void testWriteFileSyncWithUnknownPathCreatesFile() throws Throwable {
		try {
			filesystem.writeFileSync("content", "isntThere.txt", true);
			assertTrue(new File("isntThere.txt").exists());
			verifyZeroInteractions(process);
		} finally {
			File file = new File("isntThere.txt");
			if (file.exists()) {
				file.delete();
			}
		}
	}

	@Test
	public void testWriteFileAsyncWithUnknownPathCreatesFile() throws Throwable {
		try {
			FileWriteHandler handler = mock(FileWriteHandler.class);

			filesystem.writeFile("content", "isntThere.txt", handler);

			ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
			Thread.sleep(500); // FIXME: how can we get rid off this sleep?
			verify(process).nextTick(captor.capture());

			Runnable dispatchedRunnable = TestHelper.getCaptorValue(captor);
			assertNotNull(dispatchedRunnable);
			dispatchedRunnable.run();

			ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
			verify(handler).fileWritten(contentCaptor.capture());
			String writtenContent = TestHelper.getCaptorValue(contentCaptor);
			assertThat(writtenContent, is("content"));

			assertTrue(new File("isntThere.txt").exists());

			verifyNoMoreInteractions(handler, process);
		} finally {
			File file = new File("isntThere.txt");
			if (file.exists()) {
				file.delete();
			}
		}
	}

}
