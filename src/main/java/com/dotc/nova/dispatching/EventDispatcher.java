package com.dotc.nova.dispatching;

import java.util.List;
import java.util.concurrent.*;

import com.dotc.nova.events.EventListener;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;

public class EventDispatcher {
	private static final int BUFFER_SIZE = 10000;

	private RingBuffer<EventContext> ringBuffer;
	private ClaimStrategy claimStrategy = new MultiThreadedClaimStrategy(BUFFER_SIZE);
	private WaitStrategy waitStrategy = new BlockingWaitStrategy();
	private Executor executor;

	public void init() {
		ThreadFactory threadFactory = new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "EventDispatcher");
				t.setDaemon(true);
				return t;
			}
		};
		executor = Executors.newSingleThreadExecutor(threadFactory);

		EventFactory<EventContext> eventFactory = new EventFactory<EventContext>() {

			@Override
			public EventContext newInstance() {
				return new EventContext();
			}
		};

		Disruptor<EventContext> disruptor = new Disruptor<EventContext>(eventFactory, executor, claimStrategy, waitStrategy);
		ringBuffer = disruptor.getRingBuffer();
		disruptor.handleEventsWith(new DispatchingEventHandler());
	}

	public <T> void dispatch(T event, List<EventListener> listenerList) {
		for (EventListener<T> el : listenerList) {
			final long sequence = ringBuffer.next();
			EventContext<T> eventContext = ringBuffer.get(sequence);
			eventContext.setEvent(event);
			eventContext.setListener(el);
			ringBuffer.publish(sequence);
		}
	}

	public <T> void dispatch(T event, EventListener... listenerList) {
		for (EventListener<T> el : listenerList) {
			final long sequence = ringBuffer.next();
			EventContext<T> eventContext = ringBuffer.get(sequence);
			eventContext.setEvent(event);
			eventContext.setListener(el);
			ringBuffer.publish(sequence);
		}
	}
}
