package com.dotc.nova.events.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.dotc.nova.events.EventEmitter;
import com.dotc.nova.metrics.Metrics;

public class DefaultEventMetricsCollector implements EventMetricsCollector {
	private final Metrics metrics;

	private final Map<String, Map<Object, Counter>> mapType2EventSpecificCounters;
	private final Map<String, Map<Object, Meter>> mapType2EventSpecificMeters;

	public DefaultEventMetricsCollector(Metrics metrics) {
		this.metrics = metrics;

		mapType2EventSpecificCounters = new ConcurrentHashMap<>();
		mapType2EventSpecificMeters = new ConcurrentHashMap<>();
	}

	private void remove(String type, Object event) {
		metrics.remove(EventEmitter.class, type, String.valueOf(event));
	}

	private Meter getMeter(String type, Object event) {
		Map<Object, Meter> mapEventToSpecificMeter = mapType2EventSpecificMeters.get(type);
		if (mapEventToSpecificMeter == null) {
			mapEventToSpecificMeter = new ConcurrentHashMap<>();
			mapType2EventSpecificMeters.put(type, mapEventToSpecificMeter);
		}
		Meter meter = mapEventToSpecificMeter.get(event);
		if (meter == null) {
			meter = metrics.getMeter(EventEmitter.class, type, String.valueOf(event));
			mapEventToSpecificMeter.put(event, meter);
		}
		return meter;
	}

	private Counter getCounter(String type, Object event) {
		Map<Object, Counter> mapEventToSpecificCounter = mapType2EventSpecificCounters.get(type);
		if (mapEventToSpecificCounter == null) {
			mapEventToSpecificCounter = new ConcurrentHashMap<>();
			mapType2EventSpecificCounters.put(type, mapEventToSpecificCounter);
		}
		Counter counter = mapEventToSpecificCounter.get(event);
		if (counter == null) {
			counter = metrics.getCounter(EventEmitter.class, type, String.valueOf(event));
			mapEventToSpecificCounter.put(event, counter);
		}
		return counter;
	}

	@Override
	public void eventDispatched(Object event) {
		getMeter("dispatchedEvents", event).mark();
		getMeter("dispatchedEvents", "total").mark();
	}

	@Override
	public void duplicateEventDetected(Object event) {
		getMeter("duplicateEvents", event).mark();
		getMeter("duplicateEvents", "total").mark();
	}

	@Override
	public void eventDroppedBecauseOfFullQueue(Object event) {
		getMeter("droppedEvents", event).mark();
		getMeter("droppedEvents", "total").mark();
	}

	@Override
	public void eventAddedToFullQueue(Object event) {
		getMeter("fullQueueAdds", event).mark();
		getMeter("fullQueueAdds", "total").mark();
	}

	@Override
	public void eventAddedToDispatchLaterQueue(Object event) {
		getMeter("dispatchLaterEvents", event).mark();
		getMeter("dispatchLaterEvents", "total").mark();
	}

	@Override
	public void listenerAdded(Object event) {
		getCounter("listeners", event).inc();
		getCounter("listeners", "total").inc();
	}

	@Override
	public void listenerRemoved(Object event) {
		getCounter("listeners", event).dec();
		getCounter("listeners", "total").dec();
	}

	@Override
	public void allListenersRemoved(Object event) {
		Counter counter = getCounter("listeners", event);
		long count = counter.getCount();
		getCounter("listeners", "total").dec(count);
		remove("listeners", event);
	}

	@Override
	public void eventEmittedButNoListeners(Object event) {
		getCounter("emitsWithNoListener", event).inc();
		getCounter("emitsWithNoListener", "total").inc();
	}

	@Override
	public void errorOccurredForEventDispatch(Object event) {
		getCounter("errorsOnDispatch", event).inc();
		getCounter("errorsOnDispatch", "total").inc();
	}

}
