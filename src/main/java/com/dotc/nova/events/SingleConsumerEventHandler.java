package com.dotc.nova.events;

import com.lmax.disruptor.EventHandler;

class SingleConsumerEventHandler implements EventHandler<InvocationContext> {

	@Override
	public void onEvent(InvocationContext event, long sequence, boolean endOfBatch) {
		try {
			event.getEventListener().handle(event.getData());
		} finally {
			event.reset();
		}
	}

}
