/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.filesystem;

import ch.squaredesk.nova.Nova;
import org.junit.AfterClass;
import org.junit.Assert;
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
        filesystem = Nova.builder().build().filesystem;
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
        filesystem.readFile("src/test/resources/someFile.txt")
                .doFinally(() -> countDownLatch.countDown())
                .subscribe(contents -> resultContainer[0] = contents);

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(),is(0L));
        assertThat(resultContainer[0],is("This is some content in some file."));
    }

    @Test
    public void testReadFileAsyncWithUnknownPathCausesErrorCallbackToBeInvoked() throws Throwable {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Throwable[] resultContainer = new Throwable[1];
        filesystem.readFile("doesntExist.txt")
                .doFinally(() -> countDownLatch.countDown())
                .subscribe(
                        s -> {},
                        t -> resultContainer[0] = t);

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(),is(0L));
        assertNotNull(resultContainer[0]);
    }

    @Test
    public void testWriteFileSyncWithUnknownPathCreatesFile() throws Throwable {
        filesystem.writeFileSync("content", "src/test/resources/isntThere.txt", true).subscribe();
        assertTrue(new File("src/test/resources/isntThere.txt").exists());
    }

    @Test
    public void testWriteFileAsyncWithUnknownPathCreatesFile() throws Throwable {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            filesystem.writeFile("content", "isntThere.txt")
                    .doFinally(()->countDownLatch.countDown())
                    .subscribe();

            countDownLatch.await(2,TimeUnit.SECONDS);
            assertThat(countDownLatch.getCount(),is(0L));
            assertTrue(new File("isntThere.txt").exists());
        } finally {
            File file = new File("isntThere.txt");
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Test
    public void testOverridingExistingFileDoesntLeaveAnyOldContent() throws Throwable {
        filesystem.writeFileSync("old loooong content", "bla.txt", false).blockingAwait();
        assertThat(filesystem.readFile("bla.txt").blockingGet(), is("old loooong content"));

        filesystem.writeFileSync("new content", "bla.txt", false).blockingAwait();
        assertThat(filesystem.readFile("bla.txt").blockingGet(), is("new content"));
    }

    @Test
    public void testAppendingToExistingFileDoesntDeleteExistingContent() throws Throwable {
        filesystem.writeFileSync("old loooong content\n", "bla.txt", false).blockingAwait();
        assertThat(filesystem.readFile("bla.txt").blockingGet(), is("old loooong content\n"));

        filesystem.writeFileSync("new content", "bla.txt", true).blockingAwait();
        assertThat(filesystem.readFile("bla.txt").blockingGet(), is("old loooong content\nnew content"));
    }

    @Test
    public void testReadFileFromClasspathSync() throws Throwable {
        assertThat(filesystem.readFileFromClasspath("/someFile.txt").blockingGet(),
                is("This is some content in some file."));
    }

    @Test
    public void testReadFileFromClasspathSyncWithUnknownPathThrowsException() throws Throwable {
        try {
            filesystem.readFileFromClasspath("src/test/resources/" + System.currentTimeMillis() + ".txt")
                    .blockingGet();
            Assert.fail("Exception expected");
        } catch (Exception e) {
            Assert.assertTrue(e.getCause() instanceof NoSuchFileException);
        }
    }
}
