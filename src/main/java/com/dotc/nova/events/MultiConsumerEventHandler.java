package com.dotc.nova.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;

class MultiConsumerEventHandler implements EventHandler<InvocationContext> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MultiConsumerEventHandler.class);

	@Override
	public void onEvent(InvocationContext event, long sequence, boolean endOfBatch) {
		Object[] data = event.getData();
		for (EventListener listener : event.getEventListeners()) {
			try {
				listener.handle(data);
			} catch (Exception e) {
				LOGGER.error("Caught exception, trying to invoke listener for event " + event, e);
			}
		}
	}

}
