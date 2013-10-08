package com.dotc.nova.events;

import java.util.List;
import java.util.concurrent.*;

import com.dotc.nova.events.EventDispatchConfig.InsufficientCapacityStrategy;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class EventLoop {

	private final RingBuffer<InvocationContext> ringBuffer;
	private final InsufficientCapacityStrategy insufficientCapacityStrategy;
	private final Executor dispatchLaterExecutor;

	public EventLoop(EventDispatchConfig eventDispatchConfig) {
		int eventBufferSize = com.lmax.disruptor.util.Util.ceilingNextPowerOfTwo(eventDispatchConfig.eventBufferSize);

		this.insufficientCapacityStrategy = eventDispatchConfig.queueFullStrategy;

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
				throw new IllegalArgumentException("Unsupported wait stratehy " + eventDispatchConfig.waitStrategy);
		}

		ProducerType producerType = eventDispatchConfig.multipleProducers ? ProducerType.MULTI : ProducerType.SINGLE;

		ThreadFactory dispatchLaterThreadFactory = new MyDispatchLaterThreadFactory();
		dispatchLaterExecutor = Executors.newSingleThreadExecutor(dispatchLaterThreadFactory);

		EventFactory<InvocationContext> eventFactory = new MyEventFactory();

		Disruptor<InvocationContext> disruptor = new Disruptor<InvocationContext>(eventFactory, eventBufferSize, dispatchLaterExecutor, producerType, waitStrategy);
		if (eventDispatchConfig.allowBatchProcessing) {
			disruptor.handleEventsWith(new BatchProcessingEventHandler());
		} else {
			disruptor.handleEventsWith(new ProcessingEventHandler());
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
			handleRingBufferFull(null, listener, data);
		}
	}

	public void dispatch(EventListener listener) {
		dispatch(null, listener);
	}

	private <EventType, DataType> void handleRingBufferFull(EventType event, EventListener listener, DataType... data) {
		switch (insufficientCapacityStrategy) {
			case DROP_EVENTS:
				return;
			case THROW_EXCEPTION:
				throw new InsufficientCapacityException();
			case QUEUE_EVENTS:
				dispatchLaterExecutor.execute(new MyDispatchLaterRunnable<EventType, DataType>(event, listener, data));
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

	private final class MyDispatchLaterThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "EventLoopDispatcher");
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
