package com.dotc.nova.dispatching;

import com.lmax.disruptor.EventHandler;

public class DispatchingEventHandler implements EventHandler<EventContext> {

	@Override
	public void onEvent(EventContext event, long sequence, boolean endOfBatch) throws Exception {
		event.getListener().handle(event.getEvent());
	}

}
