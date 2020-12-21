/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.filesystem;

import ch.squaredesk.nova.Nova;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.NoSuchFileException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesystemTest {
    private Filesystem filesystem;

    @BeforeEach
    void setup() {
        filesystem = Nova.builder().build().filesystem;
    }

    @AfterAll
    static void removeTestFiles() {
        String[] files = { "bla.txt", "isntThere.txt", "src/test/resources/isntThere.txt" };
        for (String s : files) {
            File f = new File(s);
            if (f.exists()) {
                f.deleteOnExit();
            }
        }
    }


    @Test
    void testReadTextFile() {
        TestSubscriber<String> testSubscriber = filesystem.readTextFile("src/test/resources/someFile.txt").test();
        testSubscriber.assertValueCount(3);
        testSubscriber.assertValues("This is some content in some file.","", "This is more content.");
    }

    @Test
    void testReadTextFileFromClasspath() {
        TestSubscriber<String> testSubscriber = filesystem.readTextFileFromClasspath("/someFile.txt").test();
        testSubscriber.assertValueCount(3);
        testSubscriber.assertValues("This is some content in some file.", "", "This is more content.");
    }

    @Test
    void testReadFileFully() {
        TestObserver<String> observer = filesystem.readTextFileFully("src/test/resources/someFile.txt")
                .test();

        observer.awaitDone(2, SECONDS);
        observer.assertValue("This is some content in some file." + System.lineSeparator() + System.lineSeparator() + "This is more content.");
    }

    @Test
    void testReadFileFullyWithUnknownPathCausesErrorCallbackToBeInvoked() {
        TestObserver<String> observer = filesystem.readTextFileFully("doesntExist.txt")
                .test();

        observer.awaitDone(2, SECONDS);
        observer.assertError(NoSuchFileException.class);
    }

    @Test
    void testWriteFileSyncWithUnknownPathCreatesFile() throws Throwable {
        filesystem.writeFileSync("content", "src/test/resources/isntThere.txt", true).subscribe();
        assertTrue(new File("src/test/resources/isntThere.txt").exists());
    }

    @Test
    void testWriteFileAsyncWithUnknownPathCreatesFile() {
        try {
            TestObserver<Void> observer = filesystem.writeFile("content", "isntThere.txt").test();

            observer.awaitDone(2, SECONDS);
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
    void testReadTextFileFullyFromClasspath() {
        assertThat(filesystem.readTextFileFullyFromClasspath("/someFile.txt").blockingGet(),
                is("This is some content in some file." + System.lineSeparator() + System.lineSeparator() + "This is more content."));
    }

    @Test
    void testReadFileFromClasspathSyncWithUnknownPathThrowsException() {
        try {
            filesystem.readTextFileFullyFromClasspath("src/test/resources/" + System.currentTimeMillis() + ".txt")
                    .blockingGet();
            Assertions.fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof NoSuchFileException);
        }
    }
}
