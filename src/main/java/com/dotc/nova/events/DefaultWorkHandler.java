package com.dotc.nova.events;

import com.lmax.disruptor.WorkHandler;

class DefaultWorkHandler implements WorkHandler<InvocationContext> {

	@Override
	public void onEvent(InvocationContext event) {
		try {
			event.getEventListener().handle(event.getData());
		} finally {
			event.reset();
		}
	}

}
