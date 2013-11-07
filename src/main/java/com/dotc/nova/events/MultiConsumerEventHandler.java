package com.dotc.nova.events;

import com.lmax.disruptor.EventHandler;

class MultiConsumerEventHandler implements EventHandler<InvocationContext> {

	@Override
	public void onEvent(InvocationContext event, long sequence, boolean endOfBatch) {
		event.getEventListener().handle(event.getData());
	}

}
