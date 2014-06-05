package com.dotc.nova.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.WorkHandler;

class DefaultWorkHandler implements WorkHandler<InvocationContext> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWorkHandler.class);

	@SuppressWarnings("unchecked")
	@Override
	public void onEvent(InvocationContext event) {
		try {
			Object[] data = event.getData();
			for (EventListener listener : event.getEventListeners()) {
				try {
					listener.handle(data);
				} catch (Exception e) {
					LOGGER.error("Caught exception, trying to invoke listener for event " + event, e);
				}
			}
		} finally {
			event.reset();
		}
	}

}
