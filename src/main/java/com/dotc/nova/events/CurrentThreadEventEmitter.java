package com.dotc.nova.events;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dotc.nova.events.metrics.EventMetricsCollector;

public class CurrentThreadEventEmitter extends EventEmitter {
	private static final Logger LOGGER = LoggerFactory.getLogger(CurrentThreadEventEmitter.class);

	public CurrentThreadEventEmitter(boolean warnOnUnhandledEvents, EventMetricsCollector metricsCollector) {
		super(warnOnUnhandledEvents, metricsCollector);
	}

	@Override
	void dispatchEventAndDataToListeners(List<EventListener> listenerList, Object event, Object... data) {
		Object[] dataToPass = data.length == 0 ? null : data;
		listenerList.forEach(listener -> {
			try {
				metricsCollector.monitorEventListenerTime(event, () -> listener.handle(dataToPass));
				metricsCollector.eventDispatched(event);
			} catch (Exception e) {
				LOGGER.error("Uncaught exception while invoking eventListener " + listener, e);
				LOGGER.error("\tparamters: " + Arrays.toString(data));
			}
		});
	}

}
