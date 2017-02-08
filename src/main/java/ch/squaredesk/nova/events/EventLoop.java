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
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static ch.squaredesk.nova.events.EventLoopConfig.DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD;
import static java.util.Objects.requireNonNull;

public class EventLoop {
	private final Logger logger = LoggerFactory.getLogger(EventLoop.class);

	private final EventMetricsCollector metricsCollector;
    private final Map<Object, IdProviderForDuplicateEventDetection> idProviderRegistry;
    private final Map<Object, Object[]> mapIdToCurrentData;

    // the source of all events
    private final Subject<InvocationContext> theSource = PublishSubject.create();
    private final ConcurrentHashMap<Object,Subject<Object[]>> eventSpecificSubjects = new ConcurrentHashMap<>();

	public EventLoop(String identifier, EventLoopConfig eventLoopConfig, Metrics metrics) {
        this.metricsCollector = new EventMetricsCollector(metrics, identifier);
        this.idProviderRegistry = new ConcurrentHashMap<>();
        this.mapIdToCurrentData = new ConcurrentHashMap<>();

        if (logger.isDebugEnabled()) {
            logger.debug("Instantiating event loop " + identifier + ", using the following configuration:");
            logger.debug("\tDispatching thread strategy:        " + eventLoopConfig.dispatchThreadStrategy);
            logger.debug("\tInsufficientCapacity strategy:      " + eventLoopConfig.insufficientCapacityStrategy);
            logger.debug("\twarn on unhandled events:           " + eventLoopConfig.warnOnUnhandledEvent);
        }

        Observable<InvocationContext> threadedSource = theSource;
        if (eventLoopConfig.dispatchThreadStrategy != DISPATCH_IN_EMITTER_THREAD) {
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
                                s.onNext(invocationContext.getData());
                            } else {
                                logger.debug("No observers for event {}, disposing subject.", invocationContext.event);
                                eventSpecificSubjects.remove(invocationContext.event);
                                // TODO: dispose the subject
                                metricsCollector.eventEmittedButNoListeners(invocationContext.event);
                                if (eventLoopConfig.warnOnUnhandledEvent) {
                                    logger.warn("No listener registered for event " + invocationContext.event
                                            + ". Discarding dispatch with parameters " + Arrays.toString(invocationContext.getData()));
                                }
                            }
                        } else {
                            metricsCollector.eventEmittedButNoListeners(invocationContext.event);
                            logger.warn("No listener registered for event " + invocationContext.event
                                    + ". Discarding dispatch with parameters " + Arrays.toString(invocationContext.getData()));
                        }
                    } catch (Exception e) {
                        logger.error("An error occurred, trying to dispatch event " + invocationContext, e);
                    }
                });

        // TODO metrics.register(new RingBufferMetricSet(ringBuffer), identifier);
        // TODO: duplication detection
    }


    /**
     ***************************
     *                         *
     * Metrics related methods *
     *                         *
     * *************************
     */
    public void enableMetricsTrackingFor(Object... events) {
        if (events != null && events.length > 0) {
            Arrays.stream(events).forEach(event -> {
                metricsCollector.setTrackingEnabled(true, event);
            });
        }
    }

    public void disableMetricsTrackingFor(Object... events) {
        if (events != null && events.length > 0) {
            Arrays.stream(events).forEach(event -> {
                metricsCollector.setTrackingEnabled(false, event);
            });
        }
    }

    /**
     *************************************
     *                                   *
     * Methods related to event handling *
     *                                   *
     * ***********************************
     */
    public void emit (Object event, Object... data) {
        // if this is an event for which duplicate detection was switched on, get the ID
        Object duplicateDetectionId = null;
        IdProviderForDuplicateEventDetection idProvider = null;
        if (event != null) {
            idProvider = idProviderRegistry.get(event);
        }
        if (idProvider != null) {
            duplicateDetectionId = idProvider.provideIdFor(data);
        }
//		if (duplicateDetectionId != null) {
//			Object currentData = mapIdToCurrentData.put(duplicateDetectionId, data);
//			if (currentData == null) {
//				 put trigger onto ringBuffer
//				putEventuallyDuplicateEventIntoRingBuffer(event, consumer, duplicateDetectionId);
//			} else {
//				logger.trace("Dropped outdated data for event {}, since a more recent update came in", event);
//				metricsCollector.duplicateEventDetected(event);
//			}
//		} else {
//			putNormalEventIntoRingBuffer(event, consumer, data);
//		}
        InvocationContext ic = new InvocationContext(event, data, duplicateDetectionId, this.mapIdToCurrentData);
        theSource.onNext(ic);
    }

//	private void putNormalEventIntoRingBuffer(Object event, Consumer<Object[]> consumer, Object... data) {
//		try {
//			long nextSequenceNumber = ringBuffer.tryNext();
//			InvocationContext ic = ringBuffer.get(nextSequenceNumber);
//			ic.setEmitInfo(event, consumer, data);
//			ringBuffer.publish(nextSequenceNumber);
//			metricsCollector.eventDispatched(event);
//		} catch (com.lmax.disruptor.InsufficientCapacityException e) {
//			handleRingBufferFull(event, consumer, data);
//		}
//	}
//
//	private void putEventuallyDuplicateEventIntoRingBuffer(Object event, Consumer<Object[]> consumer,
//														   Object duplicateDetectionId) {
//		try {
//			long nextSequenceNumber = ringBuffer.tryNext();
//			InvocationContext ic = ringBuffer.get(nextSequenceNumber);
//			ic.setEmitInfo(event, consumer, duplicateDetectionId, mapIdToCurrentData);
//			ringBuffer.publish(nextSequenceNumber);
//			metricsCollector.eventDispatched(event);
//		} catch (com.lmax.disruptor.InsufficientCapacityException e) {
//			handleRingBufferFull(event, consumer, mapIdToCurrentData.remove(duplicateDetectionId));
//		}
//	}

    //	private void handleRingBufferFull(Object event, Consumer<Object[]> consumer, Object... data) {
//		switch (eventDispatchConfig.insufficientCapacityStrategy) {
//			case DROP_EVENTS:
//				logger.trace("RingBuffer {} full. Dropping event {} with parameters {}", identifier, event, Arrays.toString(data));
//				metricsCollector.eventDroppedBecauseOfFullQueue(event);
//				return;
//			case THROW_EXCEPTION:
//				logger.trace("RingBuffer {} full. Event {} with parameters {}", identifier, event, Arrays.toString(data));
//				metricsCollector.eventAddedToFullQueue(event);
//				throw new InsufficientCapacityException(event, data);
//			case QUEUE_EVENTS:
//				dispatchLaterExecutor.execute(new MyDispatchLaterRunnable(event, consumer, data));
//				logger.trace("RingBuffer {} full. Queued event {} for later processing", identifier, event);
//				metricsCollector.eventAddedToDispatchLaterQueue(event);
//				return;
//			case WAIT_UNTIL_SPACE_AVAILABLE:
//				long start = System.nanoTime();
//				long nextSequenceNumber = ringBuffer.next();
//				long stop = System.nanoTime();
//				metricsCollector.waitedForEventToBeDispatched(event, stop - start);
//				InvocationContext ic = ringBuffer.get(nextSequenceNumber);
//				ic.setEmitInfo(event, consumer, data);
//				ringBuffer.publish(nextSequenceNumber);
//				metricsCollector.eventDispatched(event);
//				return;
//		}
//	}
//
    public void registerIdProviderForDuplicateEventDetection(Object event,
                                                             IdProviderForDuplicateEventDetection duplicateDetectionIdProvider) {
        idProviderRegistry.put(event, duplicateDetectionIdProvider);
    }

    public void removeIdProviderForDuplicateEventDetection(Object event) {
        idProviderRegistry.remove(event);
    }

	public Flowable<Object[]> observe (Object event) {
		requireNonNull(event, "event must not be null");
        Subject<Object[]> eventSpecificSubject = eventSpecificSubjects.computeIfAbsent(event, key -> {
            PublishSubject<Object[]> ps = PublishSubject.create();
            return ps;
        });
        return eventSpecificSubject.toFlowable(BackpressureStrategy.BUFFER).doFinally(() -> {
            if (!eventSpecificSubject.hasObservers()) {
                logger.info("No observers left for event {}, nuking subject...",event);
                eventSpecificSubject.onComplete();
                eventSpecificSubjects.remove(event);
            }
        });
	}

	public Single<Object[]> single (Object event) {
        return observe(event)
                .first(new Object[0])
                .doFinally(() -> {
                    System.out.println("The single has finished!!!");
                });
	}

	// package private for testing
    Subject<Object[]> subjectFor(Object event) {
		requireNonNull (event,"event must not be null");
        return eventSpecificSubjects.get(event);
	}

}
