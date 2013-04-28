package com.dotc.nova;

import java.util.List;
import java.util.concurrent.*;

import com.dotc.nova.events.EventHandler;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;

public class ProcessingLoop {
	private static final int BUFFER_SIZE = com.lmax.disruptor.util.Util.ceilingNextPowerOfTwo(10000);

	private RingBuffer<InvocationContext> ringBuffer;
	private ClaimStrategy claimStrategy = new MultiThreadedClaimStrategy(BUFFER_SIZE);
	private WaitStrategy waitStrategy = new BlockingWaitStrategy();
	private Executor executor;

	public void init() {
		ThreadFactory threadFactory = new MyThreadFactory();
		executor = Executors.newSingleThreadExecutor(threadFactory);

		EventFactory<InvocationContext> eventFactory = new MyEventFactory();

		Disruptor<InvocationContext> disruptor = new Disruptor<InvocationContext>(eventFactory, executor, claimStrategy, waitStrategy);
		disruptor.handleEventsWith(new ProcessingEventHandler());
		ringBuffer = disruptor.start();
	}

	public void dispatch(Object event, List<EventHandler> listenerList, Object... data) {
		for (EventHandler el : listenerList) {
			long sequence = ringBuffer.next();
			InvocationContext eventContext = ringBuffer.get(sequence);
			eventContext.setEventListenerInfo(event, el, data);
			ringBuffer.publish(sequence);
		}
	}

	public <T> void dispatch(T event, EventHandler... listenerList) {
		for (EventHandler<T> el : listenerList) {
			long sequence = ringBuffer.next();
			InvocationContext eventContext = ringBuffer.get(sequence);
			eventContext.setEventListenerInfo(event, el);
			ringBuffer.publish(sequence);
		}
	}

	public void dispatch(EventHandler handler) {
		long sequence = ringBuffer.next();
		InvocationContext eventContext = ringBuffer.get(sequence);
		eventContext.setEventListenerInfo(null, handler);
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
