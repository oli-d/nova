/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.events.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import ch.squaredesk.nova.metrics.Metrics;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EventMetricsCollectorTest {
    EventMetricsCollector sut;

    Metrics metrics;

    @Before
    public void setUp() {
        metrics = new Metrics();
        sut = new EventMetricsCollector(metrics, "TestCollector");
    }

    @Test
    public void onlyEventEnabledForMetricsCollectionIsConsidered() {
        sut.setTrackingEnabled(true, new MyEvent("foo"));

        sut.eventDispatched(new MyEvent("foo"));
        sut.eventDispatched(new MyEvent("bar"));

        Map<String, Metric> registeredMetrics = metrics.getMetrics();
        assertThat(registeredMetrics.size(), is(2));
        Meter totalMeter = (Meter)registeredMetrics.get("EventEmitter.TestCollector.dispatchedEvents.total");
        assertThat(totalMeter.getCount(), is(2L));
        Meter fooMeter = (Meter)registeredMetrics.get("EventEmitter.TestCollector.dispatchedEvents.foo");
        assertThat(fooMeter.getCount(), is(1L));
    }
}

class MyEvent {
    private final String s;

    public MyEvent(String s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return s;
    }
}