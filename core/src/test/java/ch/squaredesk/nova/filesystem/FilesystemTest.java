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
import io.reactivex.subscribers.TestSubscriber;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesystemTest {
    private Filesystem filesystem;

    @BeforeEach
    void setup() {
        filesystem = Nova.builder().build().filesystem;
    }

    @AfterAll
    static void removeTestFiles() throws Throwable {
        String[] files = { "bla.txt", "isntThere.txt", "src/test/resources/isntThere.txt" };
        for (String s : files) {
            File f = new File(s);
            if (f.exists()) {
                f.deleteOnExit();
            }
        }
    }


    @Test
    void testReadTextFile() throws Throwable {
        TestSubscriber<String> testSubscriber = filesystem.readTextFile("src/test/resources/someFile.txt").test();
        testSubscriber.assertValueCount(3);
        testSubscriber.assertValues("This is some content in some file.","", "This is more content.");
    }

    @Test
    void testReadTextFileFromClasspath() throws Throwable {
        TestSubscriber<String> testSubscriber = filesystem.readTextFileFromClasspath("/someFile.txt").test();
        testSubscriber.assertValueCount(3);
        testSubscriber.assertValues("This is some content in some file.", "", "This is more content.");
    }

    @Test
    void testReadFileFully() throws Throwable {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        String[] resultContainer = new String[1];
        filesystem.readTextFileFully("src/test/resources/someFile.txt")
                .doFinally(countDownLatch::countDown)
                .subscribe(contents -> resultContainer[0] = contents);

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(),is(0L));
        assertThat(resultContainer[0],is("This is some content in some file.\n\nThis is more content."));
    }

    @Test
    void testReadFileFullyWithUnknownPathCausesErrorCallbackToBeInvoked() throws Throwable {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Throwable[] resultContainer = new Throwable[1];
        filesystem.readTextFileFully("doesntExist.txt")
                .doFinally(countDownLatch::countDown)
                .subscribe(
                        s -> {},
                        t -> resultContainer[0] = t);

        countDownLatch.await(2, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(),is(0L));
        assertNotNull(resultContainer[0]);
    }

    @Test
    void testWriteFileSyncWithUnknownPathCreatesFile() throws Throwable {
        filesystem.writeFileSync("content", "src/test/resources/isntThere.txt", true).subscribe();
        assertTrue(new File("src/test/resources/isntThere.txt").exists());
    }

    @Test
    void testWriteFileAsyncWithUnknownPathCreatesFile() throws Throwable {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            filesystem.writeFile("content", "isntThere.txt")
                    .doFinally(countDownLatch::countDown)
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
    void testOverridingExistingFileDoesntLeaveAnyOldContent() throws Throwable {
        filesystem.writeFileSync("old loooong content", "bla.txt", false).blockingAwait();
        assertThat(filesystem.readTextFileFully("bla.txt").blockingGet(), is("old loooong content"));

        filesystem.writeFileSync("new content", "bla.txt", false).blockingAwait();
        assertThat(filesystem.readTextFileFully("bla.txt").blockingGet(), is("new content"));
    }

    @Test
    void testAppendingToExistingFileDoesntDeleteExistingContent() throws Throwable {
        filesystem.writeFileSync("old loooong content\n", "bla.txt", false).blockingAwait();
        assertThat(filesystem.readTextFileFully("bla.txt").blockingGet(), is("old loooong content\n"));

        filesystem.writeFileSync("new content", "bla.txt", true).blockingAwait();
        assertThat(filesystem.readTextFileFully("bla.txt").blockingGet(), is("old loooong content\nnew content"));
    }

    @Test
    void testReadTextFileFullyFromClasspath() throws Throwable {
        assertThat(filesystem.readTextFileFullyFromClasspath("/someFile.txt").blockingGet(),
                is("This is some content in some file.\n\nThis is more content."));
    }

    @Test
    void testReadFileFromClasspathSyncWithUnknownPathThrowsException() throws Throwable {
        try {
            filesystem.readTextFileFullyFromClasspath("src/test/resources/" + System.currentTimeMillis() + ".txt")
                    .blockingGet();
            Assertions.fail("Exception expected");
        } catch (Exception e) {
            Assertions.assertTrue(e.getCause() instanceof NoSuchFileException);
        }
    }
}
