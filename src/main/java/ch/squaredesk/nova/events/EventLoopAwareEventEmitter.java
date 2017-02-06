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

import ch.squaredesk.nova.events.metrics.RingBufferMetricSet;
import ch.squaredesk.nova.metrics.Metrics;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

public class EventLoopAwareEventEmitter extends EventEmitter {
	private final Logger logger = LoggerFactory.getLogger(EventLoopAwareEventEmitter.class);
    private final String identifier;
    private final EventDispatchConfig eventDispatchConfig;
    private final Map<Object, IdProviderForDuplicateEventDetection> idProviderRegistry;
    private final Map<Object, Object[]> mapIdToCurrentData;
    private final Executor dispatchExecutor;
    private final Executor dispatchLaterExecutor;
    private final RingBuffer<InvocationContext> ringBuffer;

    public EventLoopAwareEventEmitter(
			String identifier,
			EventDispatchConfig eventDispatchConfig,
			Metrics metrics) {
		super(identifier, metrics, eventDispatchConfig.warnOnUnhandledEvent);
        this.identifier = identifier;
        this.eventDispatchConfig = eventDispatchConfig;
        this.idProviderRegistry = new ConcurrentHashMap<>();
        this.mapIdToCurrentData = new ConcurrentHashMap<>();
        int eventBufferSize = com.lmax.disruptor.util.Util.ceilingNextPowerOfTwo(eventDispatchConfig.eventBufferSize);

        if (logger.isDebugEnabled()) {
            logger.debug("Instantiating event loop " + identifier + ", using the following configuration:");
            logger.debug("\tRingBuffer size:                    " + eventBufferSize);
            logger.debug("\tDispatching thread strategy:        " + eventDispatchConfig.dispatchThreadStrategy);
            logger.debug("\tProducer strategy:                  " + eventDispatchConfig.producerStrategy);
            logger.debug("\tInsufficientCapacity strategy:      " + eventDispatchConfig.insufficientCapacityStrategy);
            logger.debug("\tWait strategy:                      " + eventDispatchConfig.waitStrategy);
            logger.debug("\t# consumers (threads):              " + eventDispatchConfig.numberOfConsumers);
            logger.debug("\twarn on unhandled events:           " + eventDispatchConfig.warnOnUnhandledEvent);
        }

        WaitStrategy waitStrategy;
        switch (eventDispatchConfig.waitStrategy) {
            case MIN_CPU_USAGE:
                waitStrategy = new BlockingWaitStrategy();
                break;
            case MIN_LATENCY:
                waitStrategy = new BusySpinWaitStrategy();
                break;
            case LOW_CPU_DEFAULT_LATENCY:
                waitStrategy = new SleepingWaitStrategy();
                break;
            case LOW_LATENCY_DEFAULT_CPU:
                waitStrategy = new YieldingWaitStrategy();
                break;
            default:
                throw new IllegalArgumentException("Unsupported wait strategy " + eventDispatchConfig.waitStrategy);
        }

        ProducerType producerType = eventDispatchConfig.producerStrategy == EventDispatchConfig.ProducerStrategy.MULTIPLE ?
                ProducerType.MULTI : ProducerType.SINGLE;

        ThreadFactory dispatchThreadFactory = new MyDispatchThreadFactory();
        dispatchExecutor = Executors.newFixedThreadPool(eventDispatchConfig.numberOfConsumers, dispatchThreadFactory);

        dispatchLaterExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "EventLoopDispatchLater/" + identifier);
            t.setDaemon(true);
            return t;
        });

        Disruptor<InvocationContext> disruptor = new Disruptor<>(
                () -> new InvocationContext(),
                eventBufferSize,
                dispatchExecutor,
                producerType,
                waitStrategy);
        disruptor.handleExceptionsWith(new DefaultExceptionHandler());

        // TODO: what is the difference between a WorkHandler and an EventHandler? Can we get rid off that if?
        if (eventDispatchConfig.numberOfConsumers == 1) {
            disruptor.handleEventsWith(new SingleConsumerEventHandler());
        } else {
            List<WorkHandler<InvocationContext>> workHandlers = new ArrayList<>();
            for (int i = 0; i < eventDispatchConfig.numberOfConsumers; i++) {
                workHandlers.add(new DefaultWorkHandler());
            }
            disruptor.handleEventsWithWorkerPool(workHandlers.toArray(new WorkHandler[workHandlers.size()]));
        }
        ringBuffer = disruptor.start();

        metrics.register(new RingBufferMetricSet(ringBuffer), identifier);
    }


    @Override
	void dispatchEventAndData(Consumer<Object[]> consumer, Object event, Object... data) {
		// if this is an event for which duplicate detection was switched on, get the ID
		Object duplicateDetectionId = null;
		IdProviderForDuplicateEventDetection idProvider = null;
		if (event != null) {
			idProvider = idProviderRegistry.get(event);
		}
		if (idProvider != null) {
			duplicateDetectionId = idProvider.provideIdFor(data);
		}
		if (duplicateDetectionId != null) {
			Object currentData = mapIdToCurrentData.put(duplicateDetectionId, data);
			if (currentData == null) {
				// put trigger onto ringBuffer
				putEventuallyDuplicateEventIntoRingBuffer(event, consumer, duplicateDetectionId);
			} else {
				logger.trace("Dropped outdated data for event {}, since a more recent update came in", event);
				metricsCollector.duplicateEventDetected(event);
			}
		} else {
			putNormalEventIntoRingBuffer(event, consumer, data);
		}
	}

	private void putNormalEventIntoRingBuffer(Object event, Consumer<Object[]> consumer, Object... data) {
		try {
			long nextSequenceNumber = ringBuffer.tryNext();
			InvocationContext ic = ringBuffer.get(nextSequenceNumber);
			ic.setEmitInfo(event, consumer, data);
			ringBuffer.publish(nextSequenceNumber);
			metricsCollector.eventDispatched(event);
		} catch (com.lmax.disruptor.InsufficientCapacityException e) {
			handleRingBufferFull(event, consumer, data);
		}
	}

	private void putEventuallyDuplicateEventIntoRingBuffer(Object event, Consumer<Object[]> consumer,
														   Object duplicateDetectionId) {
		try {
			long nextSequenceNumber = ringBuffer.tryNext();
			InvocationContext ic = ringBuffer.get(nextSequenceNumber);
			ic.setEmitInfo(event, consumer, duplicateDetectionId, mapIdToCurrentData);
			ringBuffer.publish(nextSequenceNumber);
			metricsCollector.eventDispatched(event);
		} catch (com.lmax.disruptor.InsufficientCapacityException e) {
			handleRingBufferFull(event, consumer, mapIdToCurrentData.remove(duplicateDetectionId));
		}
	}

	private void handleRingBufferFull(Object event, Consumer<Object[]> consumer, Object... data) {
		switch (eventDispatchConfig.insufficientCapacityStrategy) {
			case DROP_EVENTS:
				logger.trace("RingBuffer {} full. Dropping event {} with parameters {}", identifier, event, Arrays.toString(data));
				metricsCollector.eventDroppedBecauseOfFullQueue(event);
				return;
			case THROW_EXCEPTION:
				logger.trace("RingBuffer {} full. Event {} with parameters {}", identifier, event, Arrays.toString(data));
				metricsCollector.eventAddedToFullQueue(event);
				throw new InsufficientCapacityException(event, data);
			case QUEUE_EVENTS:
				dispatchLaterExecutor.execute(new MyDispatchLaterRunnable(event, consumer, data));
				logger.trace("RingBuffer {} full. Queued event {} for later processing", identifier, event);
				metricsCollector.eventAddedToDispatchLaterQueue(event);
				return;
			case WAIT_UNTIL_SPACE_AVAILABLE:
				long start = System.nanoTime();
				long nextSequenceNumber = ringBuffer.next();
				long stop = System.nanoTime();
				metricsCollector.waitedForEventToBeDispatched(event, stop - start);
				InvocationContext ic = ringBuffer.get(nextSequenceNumber);
				ic.setEmitInfo(event, consumer, data);
				ringBuffer.publish(nextSequenceNumber);
				metricsCollector.eventDispatched(event);
				return;
		}
	}

	public void registerIdProviderForDuplicateEventDetection(Object event,
															 IdProviderForDuplicateEventDetection duplicateDetectionIdProvider) {
		idProviderRegistry.put(event, duplicateDetectionIdProvider);
	}

	public void removeIdProviderForDuplicateEventDetection(Object event) {
		idProviderRegistry.remove(event);
	}

	private class MyDispatchLaterRunnable implements Runnable {
		public final Object event;
		public final Consumer<Object[]> consumer;
		public final Object[] data;

		public MyDispatchLaterRunnable(Object event, Consumer<Object[]> consumer, Object... data) {
			this.event = event;
			this.consumer = consumer;
			this.data = data;
		}

		@Override
		public void run() {
			long nextSequenceNumber = ringBuffer.next();
			InvocationContext ic = ringBuffer.get(nextSequenceNumber);
			ic.setEmitInfo(event, consumer, data);
			ringBuffer.publish(nextSequenceNumber);
		}
	}

	private final class MyDispatchThreadFactory implements ThreadFactory {
		private int numInstances = 0;

		@Override
		public synchronized Thread newThread(Runnable r) {
			Thread t = new Thread(r, "EventLoopDispatcher/" + identifier + (numInstances++));
			t.setDaemon(true);
			return t;
		}
	}

	public static class InsufficientCapacityException extends RuntimeException {
		public final Object event;
		public final Object[] data;

		public InsufficientCapacityException(Object event, Object... data) {
			this.event = event;
			this.data = data;
		}

		@Override
		public String toString() {
			return String.format("InsufficientCapacityException [event=%s, data=%s]", event, data);
		}

	}
}
