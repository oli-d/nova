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
import com.codahale.metrics.Meter;

import java.util.concurrent.TimeUnit;

public class EventMetricsCollector {
	private final Metrics metrics;
	private final String identifierPrefix;

	public EventMetricsCollector(Metrics metrics, String identifierPrefix) {
		this.metrics = metrics;
		this.identifierPrefix = "EventLoop".equalsIgnoreCase(identifierPrefix) ? identifierPrefix : "EventLoop." + identifierPrefix;
	}


	public void eventDispatched(Object event) {
		String eventString = String.valueOf(event);
        metrics.getMeter(identifierPrefix,"dispatchedEvents", eventString).mark();
		metrics.getMeter(identifierPrefix,"dispatchedEvents", "total").mark();
	}

    public void eventSubjectAdded (Object event) {
        metrics.getCounter(identifierPrefix, "eventSubjects", "total").inc();
    }

	public void eventSubjectRemoved(Object event) {
        metrics.getCounter(identifierPrefix,"eventSubjects", "total").dec();
    }

	public void eventEmittedButNoObservers(Object event) {
        String eventString = String.valueOf(event);
        metrics.getCounter(identifierPrefix,"emitsWithNoListener", eventString).inc();
        metrics.getCounter(identifierPrefix,"emitsWithNoListener", "total").inc();
    }
}
