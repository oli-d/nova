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
import com.codahale.metrics.Gauge;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class EventLoop {
    static final String DUMMY_NEXT_TICK_EVENT_PREFIX = "ProcessNextTickDummyEvent";
    static final String DUMMY_TIMEOUT_EVENT_PREFIX = "TimersTimeoutDummyEvent";
    static final String DUMMY_INTERVAL_EVENT_PREFIX = "TimersIntervalDummyEvent";

	private final Logger logger = LoggerFactory.getLogger(EventLoop.class);

	// counter for dummy event creation
	private final AtomicLong counter = new AtomicLong();

	// metrics
	private final EventMetricsCollector metricsCollector;

    // the source of all events
    private final LinkedBlockingQueue<DispatchContext> sourceQueue;

    // the event specific objects (for performance)
    private final ConcurrentHashMap<Object,Consumer<Object[]>> eventSpecificListeners;
    private final ConcurrentHashMap<Object,Observable<Object[]>> eventSpecificObservables;

    // FIXME: decide on backpressure handling. ATM there's none and if we want to leave it this way,
    // we should kick EventLoopConfig
    public EventLoop(String identifier, EventLoopConfig eventLoopConfig, Metrics metrics) {
        this.metricsCollector = new EventMetricsCollector(metrics, identifier);

        if (logger.isDebugEnabled()) {
            logger.debug("Instantiating event loop " + identifier);
            logger.debug("\tdefaultBackpressureStrategy:   " + eventLoopConfig.defaultBackpressureStrategy);
            logger.debug("\twarn on unhandled events:      " + eventLoopConfig.warnOnUnhandledEvent);
        }

        metrics.register((Gauge<Long>) EventLoop.this::getNumberOfPendingRequests,"EventLoop", identifier,"pendingEvents");

        // create the mother of all events
        Scheduler eventLoopScheduler = Schedulers.from(
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread t = new Thread(runnable, "EventLoop[" + identifier + "]");
                    t.setDaemon(true);
                    return t;
                })
        );
        sourceQueue = new LinkedBlockingQueue<>();
        eventSpecificListeners = new ConcurrentHashMap<>();
        eventSpecificObservables = new ConcurrentHashMap<>();
        Observable<DispatchContext> theSource = Observable.create(s -> {
            while (!s.isDisposed()) {
                DispatchContext dispatchContext = sourceQueue.poll(100, TimeUnit.MILLISECONDS);
                if (dispatchContext !=null) {
                    s.onNext(dispatchContext);
                }
            }
        });
        theSource
                .subscribeOn(eventLoopScheduler)
                .subscribe(eventContext -> {
                    Consumer<Object[]> listenerToInform = eventSpecificListeners.get(eventContext.event);
                    if (listenerToInform != null) {
                        try {
                            listenerToInform.accept(eventContext.data);
                            metricsCollector.eventDispatched();
                        } catch (Exception e) {
                            logger.error("Error, trying to dispatch event " + eventContext);
                        }
                    } else {
                        metricsCollector.eventEmittedButNoObservers(eventContext.event);
                        if (eventLoopConfig.warnOnUnhandledEvent) {
                            logger.warn("No listener registered for event {}", eventContext);
                        }
                    }
                });
    }

    public long getNumberOfPendingRequests() {
        return sourceQueue.size();
    }

    /**
     *************************************
     *                                   *
     * Methods related to event handling *
     *                                   *
     *************************************
     **/
    // TODO Flowables come with a recognizable performance penalty, so we use Observables here
    // TODO unfortunately, this leaves our EventLoop in the current form totally unprotected from bezerk emitters
    // TODO there are two vulnerabilities:
    // TODO 1.) the unbounded queue of events to be dispatched
    // TODO 2.) the fact that we return Observables and not Flowables to the outside world
    public void emit (Object event, Object... data) {
        requireNonNull(event, "event must not be null");
        DispatchContext dispatchContext = new DispatchContext(event, data);
        try {
            sourceQueue.put(dispatchContext);
        } catch (Exception e) {
            logger.error("Unable to dispatch " + dispatchContext,e);
        }
    }

	public Observable<Object[]> on(Object event) {
        return on(event, metricsCollector::eventSubjectAdded, metricsCollector::eventSubjectRemoved);
    }


	private Observable<Object[]> on(Object event,
                                  Consumer<Object> metricsUpdaterCreation,
                                  Consumer<Object> metricsUpdaterTearDown) {
        requireNonNull(event, "event must not be null");
        return eventSpecificObservables.computeIfAbsent(event, key -> {
            AtomicLong eventSpecificDispatchCounter = metricsCollector.getEventSpecififcDispatchCounter(event);
            Observable<Object[]>  o = Observable.create(s -> {
                eventSpecificListeners.put(event, data -> {
                    s.onNext(data);
                    eventSpecificDispatchCounter.incrementAndGet();
                });
            });

            metricsUpdaterCreation.accept(event);

            return o
                    .doFinally(() -> {
                        eventSpecificObservables.remove(event);
                        metricsUpdaterTearDown.accept(event);
                    })
                    .share();
        });
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
        String nextTickDummyEvent = DUMMY_NEXT_TICK_EVENT_PREFIX + newId;
        on(nextTickDummyEvent,metricsCollector::nextTickSet,null)
                .take(1)
                .subscribe(
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
        emit(nextTickDummyEvent);
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
        Disposable single =
                on(timeoutDummyEvent,metricsCollector::timeoutSet,null)
                .take(1)
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
        Disposable callbackInvoker =
                on(intervalDummyEvent, metricsCollector::intervalSet, null)
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
