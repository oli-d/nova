package com.dotc.nova;

import com.lmax.disruptor.EventHandler;

class ProcessingEventHandler implements EventHandler<InvocationContext> {

	@Override
	public void onEvent(InvocationContext event, long sequence, boolean endOfBatch) throws Exception {
		event.getEventListener().handle(event.getEvent());
	}

}
