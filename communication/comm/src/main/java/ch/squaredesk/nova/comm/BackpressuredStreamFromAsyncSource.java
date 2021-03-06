/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm;

import ch.squaredesk.nova.tuples.Pair;
import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The fundamental concept we are trying to follow in Nova's comm packages is that we want to offer backpressured
 * streams of messages. Such streams can easily be created from sync sources / sources that need to be polled
 * (e.g. kafka) using <code>Flowable.generate()</code>.
 *
 * For async sources, this is not so trivial, therefore we provide this helper class. It may look totally strange to
 * have an async, probably multi threaded sources and process this in a serialized, blocking, "pull style" manner,
 * but we consider the ability to apply backpressure from the consumers more important than throughput.
 *
 * Using the backpressured stream means that - per default - incoming messages will only be read as fast as the slowest
 * consumer is able to handle them, therefore it is easy to slow down the performance of a whole server. We are aware of
 * that risk, but think that RxJava provides enough easy-to-use tools to cope with that situation.
 *
 * And yes, it is super slow compared to PublishSubject (roughly 10% of the performance) or direct handler invocation
 * (roughly 1% of the performance), so it is not well suited for high frequency trading apps, but in "normal" cases,
 * we think that the performance is tolerable since request processing will anyway be orders of magnitude slower than the
 * pure dispatching.
 */
public class BackpressuredStreamFromAsyncSource<T> {
    private static final int DEFAULT_MESSAGE_BUFFER_SIZE = 1;

    private final BlockingQueue<T> queue;
    private final Runnable closeAction;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public BackpressuredStreamFromAsyncSource() {
        this (DEFAULT_MESSAGE_BUFFER_SIZE, null);
    }

    public BackpressuredStreamFromAsyncSource(Runnable closeAction) {
        this (DEFAULT_MESSAGE_BUFFER_SIZE, closeAction);
    }

    private BackpressuredStreamFromAsyncSource(int messageBufferSize, Runnable closeAction) {
        queue = new DisruptorBlockingQueue<>(messageBufferSize);
        this.closeAction = closeAction;
    }

    public void onNext(T element) {
        if (shutdown.get()) {
            throw new IllegalStateException("Stream closed");
        }

        try {
            queue.put(element);
        } catch (InterruptedException e) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();        }
    }

    public void onComplete() {
        shutdown.set(true);
    }

    public Flowable<T> toFlowable() {
        Flowable<T> theFlowable = Flowable.generate(
                () -> new Pair<>(queue, shutdown),
                (queueShutdownPair, emitter) -> {
                    T element = null;
                    while (!queueShutdownPair.item2().get() && element == null) {
                        try {
                            element = queueShutdownPair.item1().poll(100, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            emitter.onComplete();
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    if (element!=null) {
                        emitter.onNext(element);
                    } else {
                        emitter.onComplete();
                    }
                },
                queueShutdownPair -> {
                    if (closeAction!=null) {
                        closeAction.run();
                    }
                });
        return theFlowable.subscribeOn(Schedulers.io());
    }

}
