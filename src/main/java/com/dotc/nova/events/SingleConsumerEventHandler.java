package com.dotc.nova.events;

import com.dotc.nova.events.metrics.EventMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;

class SingleConsumerEventHandler implements EventHandler<InvocationContext> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SingleConsumerEventHandler.class);

	private final EventMetricsCollector metricsCollector;

	public SingleConsumerEventHandler(EventMetricsCollector metricsCollector) {
		this.metricsCollector = metricsCollector;
	}

	@Override
	public void onEvent(InvocationContext event, long sequence, boolean endOfBatch) {
		try {
			Object[] data = event.getData();
			for (EventListener listener : event.getEventListeners()) {
				try {
					metricsCollector.monitorEventListenerTime(event.getEvent(), () -> listener.handle(data));
				} catch (Exception e) {
					LOGGER.error("Caught exception, trying to invoke listener for event " + event, e);
				}
			}
		} finally {
			event.reset();
		}
	}

}
