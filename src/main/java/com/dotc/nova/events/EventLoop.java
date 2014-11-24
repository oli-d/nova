package com.dotc.nova.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dotc.nova.events.EventDispatchConfig.InsufficientCapacityStrategy;
import com.dotc.nova.events.EventDispatchConfig.MultiConsumerDispatchStrategy;
import com.dotc.nova.events.EventDispatchConfig.ProducerStrategy;
import com.dotc.nova.events.metrics.EventMetricsCollector;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

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
				LOGGER.debug("\t# multi consumer dispatch strategy: " + eventDispatchConfig.multiConsumerDispatchStrategy);
			}
			LOGGER.debug("\twarn on unhandled events:           " + eventDispatchConfig.warnOnUnhandledEvent);
		}
		this.metricsCollector = metricsCollector;

		this.insufficientCapacityStrategy = eventDispatchConfig.insufficientCapacityStrategy;

		WaitStrategy waitStrategy = null;
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

		Disruptor<InvocationContext> disruptor = new Disruptor<>(eventFactory, eventBufferSize, dispatchExecutor, producerType,
				waitStrategy);
		disruptor.handleExceptionsWith(new DefaultExceptionHandler());
		if (eventDispatchConfig.numberOfConsumers == 1) {
			EventHandler<InvocationContext> handler = new SingleConsumerEventHandler();
			disruptor.handleEventsWith(handler);
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

	public void dispatch(EventListener listener) {
		dispatch(null, listener);
	}

	public <EventType, DataType> void dispatch(EventType event, List<EventListener> listenerList) {
		dispatch(event, listenerList, (DataType[]) null);
	}

	public <EventType, DataType> void dispatch(EventType event, List<EventListener> listenerList, DataType... data) {
		dispatch(event, listenerList.toArray(new EventListener[listenerList.size()]), data);
	}

	public <EventType, DataType> void dispatch(EventType event, EventListener[] listenerArray) {
		dispatch(event, listenerArray, (DataType[]) null);
	}

	public <EventType, DataType> void dispatch(EventType event, EventListener listener, DataType... data) {
		dispatch(event, new EventListener[] { listener }, data);
	}

	public <EventType, DataType> void dispatch(EventType event, EventListener[] listeners, DataType... data) {
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

	private <EventType, DataType> void putNormalEventIntoRingBuffer(EventType event, EventListener[] listeners, DataType... data) {
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

	private <EventType> void putEventuallyDuplicateEventIntoRingBuffer(EventType event, EventListener[] listeners,
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

	private <EventType, DataType> void handleRingBufferFull(EventType event, EventListener[] listeners, DataType... data) {
		switch (insufficientCapacityStrategy) {
			case DROP_EVENTS:
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("RingBuffer " + identifier + " full. Dropping event " + event + " with parameters "
							+ Arrays.toString(data));
				}
				metricsCollector.eventDroppedBecauseOfFullQueue(event);
				return;
			case THROW_EXCEPTION:
				LOGGER.trace("RingBuffer " + identifier + " full. Event " + event + " with parameters " + Arrays.toString(data));
				metricsCollector.eventAddedToFullQueue(event);
				throw new InsufficientCapacityException(event, data);
			case QUEUE_EVENTS:
				dispatchLaterExecutor.execute(new MyDispatchLaterRunnable<EventType, DataType>(event, listeners, data));
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("RingBuffer " + identifier + " full. Queued event " + event + " for later processing");
				}
				metricsCollector.eventAddedToDispatchLaterQueue(event);
				return;
			case WAIT_UNTIL_SPACE_AVAILABLE:
				long nextSequenceNumber = ringBuffer.next();
				InvocationContext ic = ringBuffer.get(nextSequenceNumber);
				ic.setEventListenerInfo(event, listeners, data);
				ringBuffer.publish(nextSequenceNumber);
				metricsCollector.eventDispatched(event);
				return;
		}
	}

	public void registerIdProviderForDuplicateEventDetection(Object event, IdProviderForDuplicateEventDetection duplicateDetectionIdProvider) {
		idProviderRegistry.put(event, duplicateDetectionIdProvider);
	}

	public void removeIdProviderForDuplicateEventDetection(Object event) {
		idProviderRegistry.remove(event);
	}

	private class MyDispatchLaterRunnable<EventType, DataType> implements Runnable {
		public final EventType event;
		public final EventListener[] listeners;
		public final DataType[] data;

		public MyDispatchLaterRunnable(EventType event, EventListener[] listeners, DataType... data) {
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
