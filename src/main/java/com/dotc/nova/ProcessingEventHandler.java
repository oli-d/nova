package com.dotc.nova;

import com.lmax.disruptor.EventHandler;

class ProcessingEventHandler implements EventHandler<InvocationContext> {

	@Override
	public void onEvent(InvocationContext event, long sequence, boolean endOfBatch) throws Exception {
		if (event.getData().length == 0) {
			event.getEventListener().handle(null);
		} else {
			event.getEventListener().handle(event.getData());
		}
	}
}
