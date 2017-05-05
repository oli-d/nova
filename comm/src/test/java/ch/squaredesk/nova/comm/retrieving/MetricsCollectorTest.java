/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.retrieving;

import ch.squaredesk.nova.metrics.Metrics;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricsCollectorTest {
    private Metrics metrics = new Metrics();

    private MetricsCollector sut = new MetricsCollector("test", metrics);

    @Test
    void instanceCannotBeCreatedWithoutMetrics()  {
        Throwable t = assertThrows(NullPointerException.class,
                () -> new MetricsCollector("test", null));
        assertThat(t.getMessage(), containsString("metrics"));
    }

    @Test
    void messageReceivedCanBeInvokedWithNull()  {
        sut.messageReceived(null);

        assertThat(metrics.getMeter("messageReceiver", "test", "received","total").getCount(), is(1L));
        assertThat(metrics.getMeter("messageReceiver", "test", "received","null").getCount(), is(1L));
    }

    @Test
    void messageReceived() {
        sut.messageReceived("destination1");

        assertThat(metrics.getMeter("messageReceiver", "test", "received","total").getCount(), is(1L));
        assertThat(metrics.getMeter("messageReceiver", "test", "received","destination1").getCount(), is(1L));
    }

    @Test
    void subscriptionCreated() throws Exception {
        sut.subscriptionCreated("destination2");

        assertThat(metrics.getCounter("messageReceiver", "test", "subscriptions", "total").getCount(), is(1L));
        assertThat(metrics.getCounter("messageReceiver", "test", "subscriptions", "destination2").getCount(), is(1L));
    }

    @Test
    void subscriptionCreatedCanBeInvokedWithNull()  {
        sut.subscriptionCreated(null);

        assertThat(metrics.getCounter("messageReceiver", "test", "subscriptions", "total").getCount(), is(1L));
        assertThat(metrics.getCounter("messageReceiver", "test", "subscriptions", "null").getCount(), is(1L));
    }

    @Test
    void subscriptionDestroyed()  {
        sut.subscriptionDestroyed("destination3");

        assertThat(metrics.getCounter("messageReceiver", "test", "subscriptions", "total").getCount(), is(-1L));
        assertThat(metrics.getCounter("messageReceiver", "test", "subscriptions", "destination3").getCount(), is(-1L));
    }

    @Test
    void subscriptionDestroyedCanBeInvokedWithNull() {
        sut.subscriptionDestroyed(null);

        assertThat(metrics.getCounter("messageReceiver", "test", "subscriptions", "total").getCount(), is(-1L));
        assertThat(metrics.getCounter("messageReceiver", "test", "subscriptions", "null").getCount(), is(-1L));
    }

}