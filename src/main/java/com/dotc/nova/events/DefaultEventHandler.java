package com.dotc.nova.events;

import com.lmax.disruptor.EventHandler;

class DefaultEventHandler implements EventHandler<InvocationContext> {

	@Override
	public void onEvent(InvocationContext event, long sequence, boolean endOfBatch) throws Exception {
		event.getEventListener().handle(event.getData());
	}

}
