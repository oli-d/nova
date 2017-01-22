/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.filesystem;

import ch.squaredesk.nova.Nova;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class FilesystemTest {
	private Filesystem filesystem;

	@Before
	public void setup() {
		filesystem = new Nova.Builder().build().filesystem;
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
		CountDownLatch countDownLatch = new CountDownLatch(1);
		String[] resultContainer = new String[1];
		FileReadHandler handler = new FileReadHandler() {
            @Override
            public void fileRead(String fileContents) {
                resultContainer[0] = fileContents;
                countDownLatch.countDown();
            }

            @Override
            public void errorOccurred(Throwable error) {
                countDownLatch.countDown();
            }
        };

		filesystem.readFile("src/test/resources/someFile.txt", handler);

		countDownLatch.await(2, TimeUnit.SECONDS);

		assertThat(resultContainer[0],is("This is some content in some file."));
	}

	@Test
	public void testReadFileAsyncWithUnknownPathCausesErrorCallbackToBeInvoked() throws Throwable {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Throwable[] resultContainer = new Throwable[1];
        FileReadHandler handler = new FileReadHandler() {
            @Override
            public void fileRead(String fileContents) {
                countDownLatch.countDown();
            }

            @Override
            public void errorOccurred(Throwable error) {
                resultContainer[0] = error;
                countDownLatch.countDown();
            }
        };
		filesystem.readFile("doesntExist.txt", handler);

		countDownLatch.await(2,TimeUnit.SECONDS);
		assertNotNull(resultContainer[0]);
	}

	@Test
	public void testReadFileSync() throws Throwable {
		assertThat(filesystem.readFileSync("src/test/resources/someFile.txt"), is("This is some content in some file."));
	}

	@Test(expected = NoSuchFileException.class)
	public void testReadFileSyncWithUnknownPathThrowsException() throws Throwable {
		filesystem.readFileSync("src/test/resources/" + System.currentTimeMillis() + ".txt");
	}

	@Test
	public void testWriteFileSyncWithUnknownPathCreatesFile() throws Throwable {
		filesystem.writeFileSync("content", "src/test/resources/isntThere.txt", true);
		assertTrue(new File("src/test/resources/isntThere.txt").exists());
	}

	@Test
	public void testWriteFileAsyncWithUnknownPathCreatesFile() throws Throwable {
	    CountDownLatch countDownLatch = new CountDownLatch(1);
	    String[] resultContainer = new String[1];
	    FileWriteHandler handler = new FileWriteHandler() {
            @Override
            public void fileWritten(String fileContents) {
                resultContainer[0] = fileContents;
                countDownLatch.countDown();
            }

            @Override
            public void errorOccurred(Throwable error) {
                countDownLatch.countDown();
            }
        };
		try {
			filesystem.writeFile("content", "isntThere.txt", handler);

            countDownLatch.await(2,TimeUnit.SECONDS);
            assertTrue(new File("isntThere.txt").exists());
            assertThat(resultContainer[0],is("content"));
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
	}

	@Test(expected = NoSuchFileException.class)
	public void testReadFileFromClasspathSyncWithUnknownPathThrowsException() throws Throwable {
		filesystem.readFileFromClasspathSync("src/test/resources/" + System.currentTimeMillis() + ".txt");
	}

	@Test
	public void testReadFileFromClasspath() throws Throwable {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        String[] resultContainer = new String[1];
        FileReadHandler handler = new FileReadHandler() {
            @Override
            public void fileRead(String fileContents) {
                resultContainer[0] = fileContents;
                countDownLatch.countDown();
            }

            @Override
            public void errorOccurred(Throwable error) {
                countDownLatch.countDown();
            }
        };

        filesystem.readFileFromClasspath("/someFile.txt", handler);

        countDownLatch.await(2, TimeUnit.SECONDS);
	}

	@Test
	public void testReadFileFromClasspathWithUnknownPathCausesErrorCallbackToBeInvoked() throws Throwable {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Throwable[] resultContainer = new Throwable[1];
        FileReadHandler handler = new FileReadHandler() {
            @Override
            public void fileRead(String fileContents) {
                countDownLatch.countDown();
            }

            @Override
            public void errorOccurred(Throwable error) {
                resultContainer[0] = error;
                countDownLatch.countDown();
            }
        };
        filesystem.readFileFromClasspath("doesntExist.txt", handler);

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertNotNull(resultContainer[0]);
	}

	@Test
	public void testReadFileWithLeadingPathSeparator() throws Throwable {
		String pathWithLeadingSeparatorAndDrive = getClass().getResource("/someFile.txt").getFile();
		System.out.println(pathWithLeadingSeparatorAndDrive);
		assertThat(filesystem.readFileFromClasspathSync("/someFile.txt"), is("This is some content in some file."));
	}

}
