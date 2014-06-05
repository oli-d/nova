package com.dotc.nova.events.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.dotc.nova.metrics.Metrics;

public class EventMetricsCollector {
	private final Metrics metrics;

	private final Map<String, Map<Object, Counter>> mapType2EventSpecificCounters;
	private final Map<String, Map<Object, Meter>> mapType2EventSpecificMeters;
	private final Set<Object> eventsToBeTracked;
	private final String idPrefix;

	public EventMetricsCollector(Metrics metrics, String identifierPrefix) {
		this.metrics = metrics;
		idPrefix = identifierPrefix == "EventEmitter" ? "" : "EventEmitter." + identifierPrefix;

		mapType2EventSpecificCounters = new ConcurrentHashMap<>();
		mapType2EventSpecificMeters = new ConcurrentHashMap<>();
		eventsToBeTracked = Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>());
	}

	public void setTrackingEnabled(boolean enabled, Object... events) {
		if (events == null || events.length == 0) {
			return;
		}

		for (Object event : events) {
			if (enabled) {
				eventsToBeTracked.add(event);
			} else {
				eventsToBeTracked.remove(event);
			}
		}
	}

	private void remove(String type, Object event) {
		metrics.remove(idPrefix, type, String.valueOf(event));
	}

	private Meter getMeter(String type, Object event) {
		Map<Object, Meter> mapEventToSpecificMeter = mapType2EventSpecificMeters.get(type);
		if (mapEventToSpecificMeter == null) {
			mapEventToSpecificMeter = new ConcurrentHashMap<>();
			mapType2EventSpecificMeters.put(type, mapEventToSpecificMeter);
		}

		Meter meter = mapEventToSpecificMeter.get(event);
		if (meter == null) {
			meter = metrics.getMeter(idPrefix, type, String.valueOf(event));
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
			counter = metrics.getCounter(idPrefix, type, String.valueOf(event));
			mapEventToSpecificCounter.put(event, counter);
		}
		return counter;
	}

	private boolean shouldBeTracked(Object event) {
		return event != null && eventsToBeTracked.contains(event);
	}

	public void eventDispatched(Object event) {
		if (shouldBeTracked(event)) {
			getMeter("dispatchedEvents", event).mark();
		}
		getMeter("dispatchedEvents", "total").mark();
	}

	public void duplicateEventDetected(Object event) {
		if (shouldBeTracked(event)) {
			getMeter("duplicateEvents", event).mark();
		}
		getMeter("duplicateEvents", "total").mark();
	}

	public void eventDroppedBecauseOfFullQueue(Object event) {
		if (shouldBeTracked(event)) {
			getMeter("droppedEvents", event).mark();
		}
		getMeter("droppedEvents", "total").mark();
	}

	public void eventAddedToFullQueue(Object event) {
		if (shouldBeTracked(event)) {
			getMeter("fullQueueAdds", event).mark();
		}
		getMeter("fullQueueAdds", "total").mark();
	}

	public void eventAddedToDispatchLaterQueue(Object event) {
		getMeter("dispatchLaterEvents", event).mark();
		getMeter("dispatchLaterEvents", "total").mark();
	}

	public void listenerAdded(Object event) {
		if (shouldBeTracked(event)) {
			getCounter("listeners", event).inc();
			getCounter("listeners", "total").inc();
		}
	}

	public void listenerRemoved(Object event) {
		if (shouldBeTracked(event)) {
			getCounter("listeners", event).dec();
			getCounter("listeners", "total").dec();
		}
	}

	public void allListenersRemoved(Object event) {
		if (shouldBeTracked(event)) {
			Counter counter = getCounter("listeners", event);
			long count = counter.getCount();
			getCounter("listeners", "total").dec(count);
			remove("listeners", event);
		}
	}

	public void eventEmittedButNoListeners(Object event) {
		getCounter("emitsWithNoListener", event).inc();
		if (shouldBeTracked(event)) {
			getCounter("emitsWithNoListener", "total").inc();
		}
	}

	public void errorOccurredForEventDispatch(Object event) {
		getCounter("errorsOnDispatch", event).inc();
		if (shouldBeTracked(event)) {
			getCounter("errorsOnDispatch", "total").inc();
		}
	}

}
