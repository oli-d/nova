/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events;

import ch.squaredesk.nova.Nova;
import io.reactivex.disposables.Disposable;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class TimersTest {
    private EventLoop sut;

    @Before
    public void setup() {
        sut = Nova.builder().build().eventLoop;
    }

    @Test(expected = NullPointerException.class)
    public void testSetTimeoutThrowsIfNoCallbackProvided() {
        sut.setTimeout(null, 100);
    }

    @Test
    public void testSetTimeout() throws Throwable {
        BasicConfigurator.configure();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        boolean[] invocationFlag = new boolean[1];
        Runnable callback = () -> {
            invocationFlag[0] = true;
            countDownLatch.countDown();
        };

        long startDelay = 300;
        assertNotNull(sut.setTimeout(callback, startDelay, TimeUnit.MILLISECONDS));

        countDownLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(invocationFlag[0],is(true));
    }

    @Test
    public void testClearTimeout() throws Exception {
        AtomicInteger counter = new AtomicInteger();

        Runnable callback = () -> counter.incrementAndGet();

        long startDelay = 200;
        Disposable disposable = sut.setTimeout(callback, startDelay);

        disposable.dispose();
        Thread.sleep(2 * startDelay);
        assertNotNull(disposable);

        assertThat(counter.intValue(), is(0));
    }

    @Test(expected = NullPointerException.class)
    public void testSetIntervalThrowsIfNoCallbackProvided() {
        sut.setInterval(null, 100);
    }

    @Test(expected = NullPointerException.class)
    public void testSetIntervalThrowsIfNoTimeUnitProvided() {
        sut.setInterval(() -> { },0, 100L, null);
    }

    @Test
    public void testSetInterval() throws Throwable {
        AtomicInteger counter = new AtomicInteger();

        long startDelay = 200;
        Disposable disposable = sut.setInterval(() -> counter.incrementAndGet(), startDelay, startDelay, TimeUnit.MILLISECONDS);
        assertNotNull(disposable);
        Thread.sleep((4 * startDelay) + 100);
        disposable.dispose();

        assertThat(counter.intValue(), is(4));
    }

    @Test
    public void testClearInterval() throws Exception {
        AtomicInteger counter = new AtomicInteger();

        long startDelay = 200;
        Disposable disposable = sut.setInterval(()->counter.incrementAndGet(), startDelay);
        disposable.dispose();
        Thread.sleep(2 * startDelay);
        assertNotNull(disposable);

        assertThat(counter.intValue(), is(0));
    }

}
