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

import ch.squaredesk.nova.events.metrics.EventMetricsCollector;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

public class EventLoop {
    static final String DUMMY_NEXT_TICK_EVENT_PREFIX = "ProcessNextTickDummyEvent";
    static final String DUMMY_TIMEOUT_EVENT_PREFIX = "TimersTimeoutDummyEvent";
    static final String DUMMY_INTERVAL_EVENT_PREFIX = "TimersIntervalDummyEvent";

    private final AtomicLong counter = new AtomicLong();
	private final Logger logger = LoggerFactory.getLogger(EventLoop.class);
	private final EventMetricsCollector metricsCollector;

    // the source of all events
    private final Subject<InvocationContext> theSource = PublishSubject.create();
    private final ConcurrentHashMap<Object,Subject<Object[]>> eventSpecificSubjects = new ConcurrentHashMap<>();
    private final EventLoopConfig eventLoopConfig;

    public EventLoop(String identifier, EventLoopConfig eventLoopConfig, Metrics metrics) {
        this.eventLoopConfig = eventLoopConfig;
        this.metricsCollector = new EventMetricsCollector(metrics, identifier);

        if (logger.isDebugEnabled()) {
            logger.debug("Instantiating event loop " + identifier + ", using the following configuration:");
            logger.debug("\tDispatching in emitter thread: " + eventLoopConfig.dispatchInEmitterThread);
            logger.debug("\tdefaultBackpressureStrategy:   " + eventLoopConfig.defaultBackpressureStrategy);
            logger.debug("\twarn on unhandled events:      " + eventLoopConfig.warnOnUnhandledEvent);
        }

        Observable<InvocationContext> threadedSource = theSource;
        if (!eventLoopConfig.dispatchInEmitterThread) {
            Executor dispatchExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "EventLoop/" + identifier);
                t.setDaemon(true);
                return t;
            });

            threadedSource = theSource.observeOn(Schedulers.from(dispatchExecutor));
        }

        Disposable sourceDisposable = threadedSource
                .subscribe(invocationContext -> {
                    try {
                        Subject<Object[]> s = eventSpecificSubjects.get(invocationContext.event);
                        if (s!=null) {
                            if (s.hasObservers()) {
                                metricsCollector.eventDispatched(invocationContext.event);
                                s.onNext(invocationContext.data);
                            } else {
                                logger.debug("No observers for event {}, disposing subject.", invocationContext.event);
                                if (eventSpecificSubjects.remove(invocationContext.event)!=null) {
                                    metricsCollector.eventSubjectRemoved(invocationContext.event);
                                }
                                metricsCollector.eventEmittedButNoObservers(invocationContext.event);
                                if (eventLoopConfig.warnOnUnhandledEvent) {
                                    logger.warn("No listener registered for event " + invocationContext.event
                                            + ". Discarding dispatch with parameters " + Arrays.toString(invocationContext.data));
                                }
                            }
                        } else {
                            metricsCollector.eventEmittedButNoObservers(invocationContext.event);
                            logger.warn("No listener registered for event " + invocationContext.event
                                    + ". Discarding dispatch with parameters " + Arrays.toString(invocationContext.data));
                        }
                    } catch (Exception e) {
                        logger.error("An error occurred, trying to dispatch event " + invocationContext, e);
                    }
                });
    }

    /**
     *************************************
     *                                   *
     * Methods related to event handling *
     *                                   *
     *************************************
     **/
    public void emit (Object event, Object... data) {
        requireNonNull(event, "event must not be null");
        theSource.onNext(new InvocationContext(event, data));
    }

	public Flowable<Object[]> on(Object event) {
		requireNonNull(event, "event must not be null");
        Subject<Object[]> eventSpecificSubject = eventSpecificSubjects.computeIfAbsent(event, key -> {
            PublishSubject<Object[]> ps = PublishSubject.create();
            metricsCollector.eventSubjectAdded(event);
            return ps;
        });
        return eventSpecificSubject.toFlowable(eventLoopConfig.defaultBackpressureStrategy).doFinally(() -> {
            if (!eventSpecificSubject.hasObservers()) {
                logger.info("No observers left for event {}, nuking subject...",event);
                eventSpecificSubject.onComplete();
                if (eventSpecificSubjects.remove(event)!=null) {
                    metricsCollector.eventSubjectRemoved(event);
                }
            }
        });
	}

	public Single<Object[]> single (Object event) {
        return on(event)
                .first(new Object[0]);
	}

	// package private for testing
    Subject<Object[]> subjectFor(Object event) {
		requireNonNull (event,"event must not be null");
        return eventSpecificSubjects.get(event);
	}

    /**
     *************************************
     *                                   *
     * Timeouts, interval and nextTick() *
     *                                   *
     *************************************
     **/
    public void nextTick(Runnable callback) {
        requireNonNull(callback, "callback must not be null");
        String newId = String.valueOf(counter.incrementAndGet());
        String timeoutDummyEvent = DUMMY_NEXT_TICK_EVENT_PREFIX + newId;
        single(timeoutDummyEvent).subscribe(
                        x -> {
                            try {
                                callback.run();
                            } catch (Throwable t) {
                                logger.error("An error occurred trying to invoke nextTick() ",t);
                            }
                        },
                        throwable -> {
                            logger.error("An error was pushed to nextTick() invoker", throwable);
                        }
                );
        emit(timeoutDummyEvent);
    }

    /**
     * To schedule execution of a one-time callback after delay milliseconds. Returns a timeoutId for possible use with clearTimeout().
     *
     * It is important to note that your callback will probably not be called in exactly <delay> ms - Nova makes no guarantees about the
     * exact timing of when the callback will fire, nor of the ordering things will fire in. The callback will be called as close as
     * possible to the time specified.
     */
    public Disposable setTimeout(Runnable callback, long delay) {
        return setTimeout(callback, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * To schedule execution of a one-time callback after a specified delay . Returns a timeoutId for possible use with clearTimeout().
     *
     * It is important to note that your callback will probably not be called in exactly <delay> - Nova makes no guarantees about the
     * exact timing of when the callback will fire, nor of the ordering things will fire in. The callback will be called as close as
     * possible to the time specified.
     */
    public Disposable setTimeout(Runnable callback, long delay, TimeUnit timeUnit) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(timeUnit, "timeUnit must not be null");
        String newId = String.valueOf(counter.incrementAndGet());
        String timeoutDummyEvent = DUMMY_TIMEOUT_EVENT_PREFIX + newId;
        Disposable single = single(timeoutDummyEvent)
                .delay(delay, timeUnit)
                .subscribe(
                        x -> {
                            try {
                                callback.run();
                            } catch (Throwable t) {
                                logger.error("An error occurred trying to invoke timeout with ID " + newId,t);
                            }
                        },
                        throwable -> {
                            logger.error("An error was pushed to timeout with ID " + newId, throwable);
                        }
                );
        emit(timeoutDummyEvent);
        return single;
    }

    /**
     * To schedule the repeated execution of callback every interval milliseconds. Returns a intervalId for possible use with clearInterval().
     *
     */
    public Disposable setInterval(Runnable callback, long interval) {
        return setInterval(callback, 0, interval, TimeUnit.MILLISECONDS);
    }

    /**
     * To schedule the repeated execution of callback. Returns a intervalId for possible use with clearInterval().
     *
     */
    public Disposable setInterval(Runnable callback, long initialDelay, long interval, TimeUnit timeUnit) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(timeUnit, "timeUnit must not be null");
        String newId = String.valueOf(counter.incrementAndGet());
        String intervalDummyEvent = DUMMY_INTERVAL_EVENT_PREFIX + newId;
        Disposable callbackInvoker = on(intervalDummyEvent)
                .subscribe(
                        x -> {
                            try {
                                callback.run();
                            } catch (Throwable t) {
                                //	logger.error("An error occurred trying to invoke timeout with ID " + newId,t);
                                // if we do not propagate the error, the interval will not stop
                                Exceptions.propagate(t);
                            }
                        });
        // we create another "wrapper Observable" to trigger the callback invocation over the EventLoop
        // thus we make sure that the event is always propagated via the EventLoop dispatch thread
        Disposable recurringEventEmitter = Observable
                .interval(initialDelay, interval, timeUnit)
                .takeWhile(counter -> !callbackInvoker.isDisposed()) // important! stops the interval when callBackInvoker dies because of error
                .doFinally(() -> callbackInvoker.dispose())
                .subscribe(
                        counter -> emit(intervalDummyEvent),
                        error -> logger.error("An error was pushed to interval dummy event emitter for intervalId " + newId, error),
                        () -> callbackInvoker.dispose());

        return recurringEventEmitter;
    }
}
