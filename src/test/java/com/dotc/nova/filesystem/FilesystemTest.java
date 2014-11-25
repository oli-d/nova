package com.dotc.nova.filesystem;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.NoSuchFileException;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FilesystemTest {
	private Filesystem filesystem;
	@Mock
	private com.dotc.nova.process.Process process;

	@Before
	public void setup() {
		filesystem = new Filesystem(process);
	}

	@AfterClass
	public static void removeTestFiles() throws Throwable {
		String[] files = { "bla.txt", "isntThere.txt", "src/test/resources/isntThere.txt" };
		for (String s : files) {
			File f = new File(s);
			if (f.exists()) {
				f.deleteOnExit();
			}
		}
	}

	@Test
	public void testReadFile() throws Throwable {
		FileReadHandler handler = mock(FileReadHandler.class);

		filesystem.readFile("src/test/resources/someFile.txt", handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(process, timeout(500)).nextTick(captor.capture());

		Runnable dispatchedRunnable = captor.getValue();
		assertNotNull(dispatchedRunnable);
		dispatchedRunnable.run();

		verify(handler).fileRead(eq("This is some content in some file."));

		verifyNoMoreInteractions(handler, process);
	}

	@Test
	public void testReadFileAsyncWithUnknownPathCausesErrorCallbackToBeInvoked() throws Throwable {
		FileReadHandler handler = mock(FileReadHandler.class);
		filesystem.readFile("doesntExist.txt", handler);
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
		filesystem.readFileSync("src/test/resources/" + System.currentTimeMillis() + ".txt");
		verifyZeroInteractions(process);
	}

	@Test
	public void testWriteFileSyncWithUnknownPathCreatesFile() throws Throwable {
		filesystem.writeFileSync("content", "src/test/resources/isntThere.txt", true);
		assertTrue(new File("src/test/resources/isntThere.txt").exists());
		verifyZeroInteractions(process);
	}

	@Test
	public void testWriteFileAsyncWithUnknownPathCreatesFile() throws Throwable {
		try {
			FileWriteHandler handler = mock(FileWriteHandler.class);

			filesystem.writeFile("content", "isntThere.txt", handler);

			ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
			verify(process, timeout(500)).nextTick(captor.capture());

			Runnable dispatchedRunnable = captor.getValue();
			assertNotNull(dispatchedRunnable);
			dispatchedRunnable.run();

			ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
			verify(handler).fileWritten(contentCaptor.capture());
			String writtenContent = contentCaptor.getValue();
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

	@Test
	public void testOverridingExistingFileDoesntLeaveAnyOldContent() throws Throwable {
		filesystem.writeFileSync("old loooong content", "bla.txt", false);
		assertThat(filesystem.readFileSync("bla.txt"), is("old loooong content"));

		filesystem.writeFileSync("new content", "bla.txt", false);
		assertThat(filesystem.readFileSync("bla.txt"), is("new content"));
	}

	@Test
	public void testAppendingToExistingFileDoesntDeleteExistingContent() throws Throwable {
		filesystem.writeFileSync("old loooong content\n", "bla.txt", false);
		assertThat(filesystem.readFileSync("bla.txt"), is("old loooong content\n"));

		filesystem.writeFileSync("new content", "bla.txt", true);
		assertThat(filesystem.readFileSync("bla.txt"), is("old loooong content\nnew content"));
	}

	@Test
	public void testReadFileFromClasspathSync() throws Throwable {
		assertThat(filesystem.readFileFromClasspathSync("/someFile.txt"), is("This is some content in some file."));
		verifyZeroInteractions(process);
	}

	@Test(expected = NoSuchFileException.class)
	public void testReadFileFromClasspathSyncWithUnknownPathThrowsException() throws Throwable {
		filesystem.readFileFromClasspathSync("src/test/resources/" + System.currentTimeMillis() + ".txt");
		verifyZeroInteractions(process);
	}

	@Test
	public void testReadFileFromClasspath() throws Throwable {
		FileReadHandler handler = mock(FileReadHandler.class);

		filesystem.readFileFromClasspath("/someFile.txt", handler);

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(process, timeout(500)).nextTick(captor.capture());

		Runnable dispatchedRunnable = captor.getValue();
		assertNotNull(dispatchedRunnable);
		dispatchedRunnable.run();

		verify(handler).fileRead(eq("This is some content in some file."));

		verifyNoMoreInteractions(handler, process);
	}

	@Test
	public void testReadFileFromClasspathWithUnknownPathCausesErrorCallbackToBeInvoked() throws Throwable {
		FileReadHandler handler = mock(FileReadHandler.class);
		filesystem.readFileFromClasspath("doesntExist.txt", handler);
		verify(handler).errorOccurred((Throwable) anyObject());

		verifyNoMoreInteractions(handler, process);
	}

	@Test
	public void testReadFileWithLeadingPathSeparator() throws Throwable {
		String pathWithLeadingSeparatorAndDrive = getClass().getResource("/someFile.txt").getFile();
		System.out.println(pathWithLeadingSeparatorAndDrive);
		assertThat(filesystem.readFileFromClasspathSync("/someFile.txt"), is("This is some content in some file."));
		verifyZeroInteractions(process);
	}

}
