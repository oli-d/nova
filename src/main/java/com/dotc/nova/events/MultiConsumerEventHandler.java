package com.dotc.nova.events;

import com.dotc.nova.events.metrics.ExecutionTimeMeasurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;

class MultiConsumerEventHandler implements EventHandler<InvocationContext> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MultiConsumerEventHandler.class);

	private final ExecutionTimeMeasurer executionTimeMeasurer;

	public MultiConsumerEventHandler(ExecutionTimeMeasurer executionTimeMeasurer) {
		this.executionTimeMeasurer = executionTimeMeasurer;
	}

	@Override
	public void onEvent(InvocationContext event, long sequence, boolean endOfBatch) {
		Object[] data = event.getData();
		for (EventListener listener : event.getEventListeners()) {
			try {
				executionTimeMeasurer.monitorRuntimeIfEnabled(event.getEvent(), () -> listener.handle(data));
			} catch (Exception e) {
				LOGGER.error("Caught exception, trying to invoke listener for event " + event, e);
			}
		}
	}

}
