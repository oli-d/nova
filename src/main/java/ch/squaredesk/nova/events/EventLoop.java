/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events;

import ch.squaredesk.nova.events.EventDispatchConfig.InsufficientCapacityStrategy;
import ch.squaredesk.nova.events.EventDispatchConfig.MultiConsumerDispatchStrategy;
import ch.squaredesk.nova.events.EventDispatchConfig.ProducerStrategy;
import ch.squaredesk.nova.events.metrics.EventMetricsCollector;
import ch.squaredesk.nova.events.metrics.RingBufferMetricSet;
import com.codahale.metrics.MetricSet;
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

public class EventLoop {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventLoop.class);

	private final String identifier;
	private final RingBuffer<InvocationContext> ringBuffer;
	private final InsufficientCapacityStrategy insufficientCapacityStrategy;
	private final Executor dispatchExecutor;
	private final Executor dispatchLaterExecutor;
	private final Map<Object, IdProviderForDuplicateEventDetection> idProviderRegistry;
	private final Map<Object, Object[]> mapIdToCurrentData;
	private final EventMetricsCollector metricsCollector;

	public EventLoop(String identifier, EventDispatchConfig eventDispatchConfig, EventMetricsCollector metricsCollector) {
		this.identifier = identifier;
		this.idProviderRegistry = new ConcurrentHashMap<>();
		this.mapIdToCurrentData = new ConcurrentHashMap<>();
		int eventBufferSize = com.lmax.disruptor.util.Util.ceilingNextPowerOfTwo(eventDispatchConfig.eventBufferSize);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Instantiating event loop " + identifier + ", using the following configuration:");
			LOGGER.debug("\tRingBuffer size:                    " + eventBufferSize);
			LOGGER.debug("\tDispatching thread strategy:        " + eventDispatchConfig.dispatchThreadStrategy);
			LOGGER.debug("\tProducer strategy:                  " + eventDispatchConfig.producerStrategy);
			LOGGER.debug("\tInsufficientCapacity strategy:      " + eventDispatchConfig.insufficientCapacityStrategy);
			LOGGER.debug("\tWait strategy:                      " + eventDispatchConfig.waitStrategy);
			LOGGER.debug("\t# consumers:                        " + eventDispatchConfig.numberOfConsumers);
			if (eventDispatchConfig.numberOfConsumers > 1) {
				LOGGER.debug("\t# multi consumer dispatch strategy: "
						+ eventDispatchConfig.multiConsumerDispatchStrategy);
			}
			LOGGER.debug("\twarn on unhandled events:           " + eventDispatchConfig.warnOnUnhandledEvent);
		}
		this.metricsCollector = metricsCollector;
		this.insufficientCapacityStrategy = eventDispatchConfig.insufficientCapacityStrategy;

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

		ProducerType producerType = eventDispatchConfig.producerStrategy == ProducerStrategy.MULTIPLE ? ProducerType.MULTI
				: ProducerType.SINGLE;

		ThreadFactory dispatchThreadFactory = new MyDispatchThreadFactory();
		dispatchExecutor = Executors.newFixedThreadPool(eventDispatchConfig.numberOfConsumers, dispatchThreadFactory);

		ThreadFactory dispatchLaterThreadFactory = new MyDispatchLaterThreadFactory();
		dispatchLaterExecutor = Executors.newSingleThreadExecutor(dispatchLaterThreadFactory);

		EventFactory<InvocationContext> eventFactory = new MyEventFactory();

		Disruptor<InvocationContext> disruptor = new Disruptor<>(eventFactory, eventBufferSize, dispatchExecutor,
				producerType, waitStrategy);
		disruptor.handleExceptionsWith(new DefaultExceptionHandler());
		if (eventDispatchConfig.numberOfConsumers == 1) {
			List<EventHandler<InvocationContext>> dummyToGetRidOffCompilerWarning = new ArrayList<>();
			dummyToGetRidOffCompilerWarning.add(new SingleConsumerEventHandler());
			disruptor.handleEventsWith(dummyToGetRidOffCompilerWarning.toArray(new EventHandler[1]));
		} else if (eventDispatchConfig.multiConsumerDispatchStrategy == MultiConsumerDispatchStrategy.DISPATCH_EVENTS_TO_ALL_CONSUMERS) {
			List<EventHandler<InvocationContext>> eventHandlers = new ArrayList<>();
			for (int i = 0; i < eventDispatchConfig.numberOfConsumers; i++) {
				eventHandlers.add(new MultiConsumerEventHandler());
			}
			disruptor.handleEventsWith(eventHandlers.toArray(new EventHandler[eventHandlers.size()]));
		} else {
			List<WorkHandler<InvocationContext>> workHandlers = new ArrayList<>();
			for (int i = 0; i < eventDispatchConfig.numberOfConsumers; i++) {
				workHandlers.add(new DefaultWorkHandler());
			}
			disruptor.handleEventsWithWorkerPool(workHandlers.toArray(new WorkHandler[workHandlers.size()]));
		}
		ringBuffer = disruptor.start();
	}

    public MetricSet getMetrics() {
        return new RingBufferMetricSet(ringBuffer);
    }

	public void dispatch(ch.squaredesk.nova.events.EventListener listener) {
		dispatch(null, listener);
	}

	public void dispatch(Object event, List<ch.squaredesk.nova.events.EventListener> listenerList) {
		dispatch(event, listenerList, (Object[]) null);
	}

	public void dispatch(Object event, List<ch.squaredesk.nova.events.EventListener> listenerList, Object... data) {
		dispatch(event, listenerList.toArray(new ch.squaredesk.nova.events.EventListener[listenerList.size()]), data);
	}

	public void dispatch(Object event, ch.squaredesk.nova.events.EventListener[] listenerArray) {
		dispatch(event, listenerArray, (Object[]) null);
	}

	public void dispatch(Object event, ch.squaredesk.nova.events.EventListener listener, Object... data) {
		dispatch(event, new ch.squaredesk.nova.events.EventListener[] { listener }, data);
	}

	public void dispatch(Object event, ch.squaredesk.nova.events.EventListener[] listeners, Object... data) {
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
				putEventuallyDuplicateEventIntoRingBuffer(event, listeners, duplicateDetectionId);
			} else {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Dropped outdated data for event " + event + ", since a more recent update came in");
				}
				metricsCollector.duplicateEventDetected(event);
			}
		} else {
			putNormalEventIntoRingBuffer(event, listeners, data);
		}
	}

	private void putNormalEventIntoRingBuffer(Object event, ch.squaredesk.nova.events.EventListener[] listeners, Object... data) {
		try {
			long nextSequenceNumber = ringBuffer.tryNext();
			InvocationContext ic = ringBuffer.get(nextSequenceNumber);
			ic.setEventListenerInfo(event, listeners, data);
			ringBuffer.publish(nextSequenceNumber);
			metricsCollector.eventDispatched(event);
		} catch (com.lmax.disruptor.InsufficientCapacityException e) {
			handleRingBufferFull(event, listeners, data);
		}
	}

	private void putEventuallyDuplicateEventIntoRingBuffer(Object event, ch.squaredesk.nova.events.EventListener[] listeners,
			Object duplicateDetectionId) {
		try {
			long nextSequenceNumber = ringBuffer.tryNext();
			InvocationContext ic = ringBuffer.get(nextSequenceNumber);
			ic.setEventListenerInfo(event, listeners, duplicateDetectionId, mapIdToCurrentData);
			ringBuffer.publish(nextSequenceNumber);
			metricsCollector.eventDispatched(event);
		} catch (com.lmax.disruptor.InsufficientCapacityException e) {
			handleRingBufferFull(event, listeners, mapIdToCurrentData.remove(duplicateDetectionId));
		}
	}

	private void handleRingBufferFull(Object event, ch.squaredesk.nova.events.EventListener[] listeners, Object... data) {
		switch (insufficientCapacityStrategy) {
			case DROP_EVENTS:
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("RingBuffer " + identifier + " full. Dropping event " + event + " with parameters "
							+ Arrays.toString(data));
				}
				metricsCollector.eventDroppedBecauseOfFullQueue(event);
				return;
			case THROW_EXCEPTION:
				LOGGER.trace("RingBuffer " + identifier + " full. Event " + event + " with parameters "
						+ Arrays.toString(data));
				metricsCollector.eventAddedToFullQueue(event);
				throw new InsufficientCapacityException(event, data);
			case QUEUE_EVENTS:
				dispatchLaterExecutor.execute(new MyDispatchLaterRunnable(event, listeners, data));
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("RingBuffer " + identifier + " full. Queued event " + event + " for later processing");
				}
				metricsCollector.eventAddedToDispatchLaterQueue(event);
				return;
			case WAIT_UNTIL_SPACE_AVAILABLE:
				long start = System.nanoTime();
				long nextSequenceNumber = ringBuffer.next();
				long stop = System.nanoTime();
				metricsCollector.waitedForEventToBeDispatched(event, stop - start);
				InvocationContext ic = ringBuffer.get(nextSequenceNumber);
				ic.setEventListenerInfo(event, listeners, data);
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
		public final ch.squaredesk.nova.events.EventListener[] listeners;
		public final Object[] data;

		public MyDispatchLaterRunnable(Object event, ch.squaredesk.nova.events.EventListener[] listeners, Object... data) {
			this.event = event;
			this.listeners = listeners;
			this.data = data;
		}

		@Override
		public void run() {
			long nextSequenceNumber = ringBuffer.next();
			InvocationContext ic = ringBuffer.get(nextSequenceNumber);
			ic.setEventListenerInfo(event, listeners, data);
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

	private final class MyDispatchLaterThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "EventLoopDispatchLater/" + identifier);
			t.setDaemon(true);
			return t;
		}
	}

	private final class MyEventFactory implements EventFactory<InvocationContext> {
		@Override
		public InvocationContext newInstance() {
			return new InvocationContext();
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
			return "InsufficientCapacityException [event=" + event + ", data=" + Arrays.toString(data) + "]";
		}

	}

}