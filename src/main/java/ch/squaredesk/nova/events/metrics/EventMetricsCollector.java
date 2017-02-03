/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events.metrics;

import com.codahale.metrics.Counter;
import ch.squaredesk.nova.metrics.Metrics;

import java.util.concurrent.TimeUnit;

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

	public void waitedForEventToBeDispatched(Object event, long waitTime) {
		String eventString = String.valueOf(event);
		if (shouldBeTracked(eventString)) {
			getTimer("waiting", "dispatchEvent", eventString).update(waitTime, TimeUnit.NANOSECONDS);
		}
        getTimer("waiting", "dispatchEvent", "total").update(waitTime, TimeUnit.NANOSECONDS);
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
			getCounter("emitters", eventString).inc();
		}
        getCounter("emitters", "total").inc();
    }

	public void listenerRemoved(Object event) {
        String eventString = String.valueOf(event);
        if (shouldBeTracked(eventString)) {
			getCounter("emitters", eventString).dec();
		}
        getCounter("emitters", "total").dec();
    }

	public void allListenersRemoved(Object event) {
        String eventString = String.valueOf(event);
        if (shouldBeTracked(eventString)) {
			remove("emitters", eventString);
        }
        Counter counter = getCounter("emitters", "total");
        counter.dec(counter.getCount());
    }

	public void eventEmittedButNoListeners(Object event) {
        String eventString = String.valueOf(event);
		if (shouldBeTracked(eventString)) {
            getCounter("emitsWithNoListener", eventString).inc();
        }
        getCounter("emitsWithNoListener", "total").inc();
    }
}
