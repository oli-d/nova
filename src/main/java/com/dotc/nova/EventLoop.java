package com.dotc.nova;

import java.util.List;
import java.util.concurrent.*;

import com.dotc.nova.events.EventListener;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class EventLoop {

	private RingBuffer<InvocationContext> ringBuffer;
	private Executor executor;

	public EventLoop(EventDispatchConfig eventDispatchConfig) {
		int eventBufferSize = com.lmax.disruptor.util.Util.ceilingNextPowerOfTwo(eventDispatchConfig.eventBufferSize);

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

		ThreadFactory threadFactory = new MyThreadFactory();
		executor = Executors.newSingleThreadExecutor(threadFactory);

		EventFactory<InvocationContext> eventFactory = new MyEventFactory();

		Disruptor<InvocationContext> disruptor = new Disruptor<InvocationContext>(eventFactory, eventBufferSize, executor, producerType, waitStrategy);
		disruptor.handleEventsWith(new ProcessingEventHandler());
		ringBuffer = disruptor.start();
	}

	public <EventType, DataType> void dispatch(EventType event, List<EventListener> listenerList, DataType... data) {
		dispatch(event, listenerList.toArray(new EventListener[listenerList.size()]), data);
	}

	public <EventType, DataType> void dispatch(EventType event, EventListener[] listenerArray, DataType... data) {
		for (EventListener el : listenerArray) {
			long sequence = ringBuffer.next();
			InvocationContext eventContext = ringBuffer.get(sequence);
			if (data.length == 0) {
				eventContext.setEventListenerInfo(event, el, (Object[]) null);
			} else {
				eventContext.setEventListenerInfo(event, el, data);
			}
			ringBuffer.publish(sequence);
		}
	}

	public void dispatch(EventListener listener) {
		long sequence = ringBuffer.next();
		InvocationContext eventContext = ringBuffer.get(sequence);
		eventContext.setEventListenerInfo(null, listener);
		ringBuffer.publish(sequence);

	}

	private final class MyThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "EventDispatcher");
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

}
