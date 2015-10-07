package com.dotc.nova.events.metrics;

import com.codahale.metrics.Counter;
import com.dotc.nova.metrics.Metrics;

public class EventMetricsCollector extends MetricsCollector {
	public EventMetricsCollector(Metrics metrics, String identifierPrefix) {
		super(metrics, "EventEmitter".equalsIgnoreCase(identifierPrefix) ? identifierPrefix :
                "EventEmitter." + identifierPrefix);
	}


	public void setTrackingEnabled(boolean enabled, Object event) {
		super.setTrackingEnabled(enabled, String.valueOf(event));
	}

	public void eventDispatched(Object event) {
        String eventString = String.valueOf(event);
        if (shouldBeTracked(eventString)) {
			getMeter("dispatchedEvents", eventString).mark();
		}
		getMeter("dispatchedEvents", "total").mark();
	}

	public void duplicateEventDetected(Object event) {
        String eventString = String.valueOf(event);
        if (shouldBeTracked(eventString)) {
			getMeter("duplicateEvents", eventString).mark();
		}
		getMeter("duplicateEvents", "total").mark();
	}

	public void eventDroppedBecauseOfFullQueue(Object event) {
        String eventString = String.valueOf(event);
        if (shouldBeTracked(eventString)) {
			getMeter("droppedEvents", eventString).mark();
		}
		getMeter("droppedEvents", "total").mark();
	}

	public void eventAddedToFullQueue(Object event) {
        String eventString = String.valueOf(event);
        if (shouldBeTracked(eventString)) {
			getMeter("fullQueueAdds", eventString).mark();
		}
		getMeter("fullQueueAdds", "total").mark();
	}

	public void eventAddedToDispatchLaterQueue(Object event) {
        String eventString = String.valueOf(event);
        if (shouldBeTracked(eventString)) {
            getMeter("dispatchLaterEvents", eventString).mark();
        }
		getMeter("dispatchLaterEvents", "total").mark();
	}

	public void listenerAdded(Object event) {
        String eventString = String.valueOf(event);
        if (shouldBeTracked(eventString)) {
			getCounter("listeners", eventString).inc();
		}
        getCounter("listeners", "total").inc();
    }

	public void listenerRemoved(Object event) {
        String eventString = String.valueOf(event);
        if (shouldBeTracked(eventString)) {
			getCounter("listeners", eventString).dec();
		}
        getCounter("listeners", "total").dec();
    }

	public void allListenersRemoved(Object event) {
        String eventString = String.valueOf(event);
        if (shouldBeTracked(eventString)) {
			remove("listeners", eventString);
        }
        Counter counter = getCounter("listeners", "total");
        counter.dec(counter.getCount());
    }

	public void eventEmittedButNoListeners(Object event) {
        String eventString = String.valueOf(event);
		if (shouldBeTracked(eventString)) {
            getCounter("emitsWithNoListener", eventString).inc();
        }
        getCounter("emitsWithNoListener", "total").inc();
    }

	public void errorOccurredForEventDispatch(Object event) {
        String eventString = String.valueOf(event);
		if (shouldBeTracked(eventString)) {
		    getCounter("errorsOnDispatch", eventString).inc();
		}
        getCounter("errorsOnDispatch", "total").inc();
	}

}
