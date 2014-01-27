package com.dotc.nova.events;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dotc.nova.events.EventDispatchConfig.BatchProcessingStrategy;
import com.dotc.nova.events.EventDispatchConfig.InsufficientCapacityStrategy;
import com.dotc.nova.events.EventDispatchConfig.MultiConsumerDispatchStrategy;
import com.dotc.nova.events.EventDispatchConfig.ProducerStrategy;
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

	public EventLoop(String identifier, EventDispatchConfig eventDispatchConfig) {
		this.identifier = identifier;
		int eventBufferSize = com.lmax.disruptor.util.Util.ceilingNextPowerOfTwo(eventDispatchConfig.eventBufferSize);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Instantiating event loop " + identifier + ", using the following configuration:");
			LOGGER.debug("\tRingBuffer size:                    " + eventBufferSize);
			LOGGER.debug("\tDispatching thread strategy:        " + eventDispatchConfig.dispatchThreadStrategy);
			LOGGER.debug("\tBatchProcessing strategy:           " + eventDispatchConfig.batchProcessingStrategy);
			LOGGER.debug("\tProducer strategy:                  " + eventDispatchConfig.producerStrategy);
			LOGGER.debug("\tInsufficientCapacity strategy:      " + eventDispatchConfig.insufficientCapacityStrategy);
			LOGGER.debug("\tWait strategy:                      " + eventDispatchConfig.waitStrategy);
			LOGGER.debug("\t# consumers:                        " + eventDispatchConfig.numberOfConsumers);
			if (eventDispatchConfig.numberOfConsumers > 1) {
				LOGGER.debug("\t# multi consumer dispatch strategy: " + eventDispatchConfig.multiConsumerDispatchStrategy);
			}
			LOGGER.debug("\twarn on unhandled events:           " + eventDispatchConfig.warnOnUnhandledEvent);
		}
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

		Disruptor<InvocationContext> disruptor = new Disruptor<InvocationContext>(eventFactory, eventBufferSize, dispatchExecutor,
				producerType, waitStrategy);
		disruptor.handleExceptionsWith(new DefaultExceptionHandler());
		if (eventDispatchConfig.numberOfConsumers == 1) {
			EventHandler eventHandler;
			if (eventDispatchConfig.batchProcessingStrategy == BatchProcessingStrategy.DROP_OUTDATED) {
				eventHandler = new EventHandlerDroppingOutdatedEvents();
			} else {
				eventHandler = new SingleConsumerEventHandler();
			}
			disruptor.handleEventsWith(eventHandler);
		} else if (eventDispatchConfig.multiConsumerDispatchStrategy == MultiConsumerDispatchStrategy.DISPATCH_EVENTS_TO_ALL_CONSUMERS) {
			EventHandler[] eventHandlers = new EventHandler[eventDispatchConfig.numberOfConsumers];
			for (int i = 0; i < eventHandlers.length; i++) {
				eventHandlers[i] = new MultiConsumerEventHandler();
			}
			disruptor.handleEventsWith(eventHandlers);
		} else {
			WorkHandler[] workHandlers = new WorkHandler[eventDispatchConfig.numberOfConsumers];
			for (int i = 0; i < workHandlers.length; i++) {
				workHandlers[i] = new DefaultWorkHandler();
			}
			disruptor.handleEventsWithWorkerPool(workHandlers);
		}
		ringBuffer = disruptor.start();
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

	public <EventType, DataType> void dispatch(EventType event, EventListener[] listenerArray, DataType... data) {
		for (EventListener el : listenerArray) {
			dispatch(event, el, data);
		}
	}

	public <EventType, DataType> void dispatch(EventType event, EventListener listener, DataType... data) {
		try {
			long nextSequenceNumber = ringBuffer.tryNext();
			InvocationContext ic = ringBuffer.get(nextSequenceNumber);
			ic.setEventListenerInfo(event, listener, data);
			ringBuffer.publish(nextSequenceNumber);
		} catch (com.lmax.disruptor.InsufficientCapacityException e) {
			handleRingBufferFull(event, listener, data);
		}
	}

	public void dispatch(EventListener listener) {
		dispatch(null, listener);
	}

	private <EventType, DataType> void handleRingBufferFull(EventType event, EventListener listener, DataType... data) {
		switch (insufficientCapacityStrategy) {
			case DROP_EVENTS:
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("RingBuffer " + identifier + " full. Dropping event " + event + " with parameters "
							+ Arrays.toString(data));
				}
				return;
			case THROW_EXCEPTION:
				LOGGER.trace("RingBuffer " + identifier + " full. Event " + event + " with parameters " + Arrays.toString(data));
				throw new InsufficientCapacityException();
			case QUEUE_EVENTS:
				dispatchLaterExecutor.execute(new MyDispatchLaterRunnable<EventType, DataType>(event, listener, data));
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("RingBuffer " + identifier + " full. Queued event " + event + " for later processing");
				}
				return;
			case WAIT_UNTIL_SPACE_AVAILABLE:
				long nextSequenceNumber = ringBuffer.next();
				InvocationContext ic = ringBuffer.get(nextSequenceNumber);
				ic.setEventListenerInfo(event, listener, data);
				ringBuffer.publish(nextSequenceNumber);
				return;
		}
	}

	private class MyDispatchLaterRunnable<EventType, DataType> implements Runnable {
		public final EventType event;
		public final EventListener listener;
		public final DataType[] data;

		public MyDispatchLaterRunnable(EventType event, EventListener listener, DataType... data) {
			this.event = event;
			this.listener = listener;
			this.data = data;
		}

		@Override
		public void run() {
			long nextSequenceNumber = ringBuffer.next();
			InvocationContext ic = ringBuffer.get(nextSequenceNumber);
			ic.setEventListenerInfo(event, listener, data);
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
		public InsufficientCapacityException() {
		}

		public InsufficientCapacityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public InsufficientCapacityException(String message, Throwable cause) {
			super(message, cause);
		}

		public InsufficientCapacityException(String message) {
			super(message);
		}

		public InsufficientCapacityException(Throwable cause) {
			super(cause);
		}
	}

}
