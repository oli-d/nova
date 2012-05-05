package com.dotc.nova;

import com.lmax.disruptor.EventHandler;

class ProcessingEventHandler implements EventHandler<InvocationContext> {

	@Override
	public void onEvent(InvocationContext event, long sequence, boolean endOfBatch) throws Exception {
		if (event.isCallbackContext()) {
			event.getCallbackToInvoke().run();
		} else {
			event.getEventListener().handle(event.getEvent());
		}
	}

}
