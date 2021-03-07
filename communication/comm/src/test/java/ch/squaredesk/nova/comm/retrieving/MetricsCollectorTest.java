/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.retrieving;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static ch.squaredesk.nova.metrics.MetricsName.buildName;
import static io.micrometer.core.instrument.Metrics.counter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MetricsCollectorTest {
    private MetricsCollector sut;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setup() {
        sut = new MetricsCollector("test");
        meterRegistry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(meterRegistry);
    }

    @AfterEach
    void destroy() {
        meterRegistry.clear();
        Metrics.globalRegistry.remove(meterRegistry);
    }

    @Test
    void messageReceivedCanBeInvokedWithNull()  {
        sut.messageReceived(null);

        assertThat(counter(buildName("test", "messageReceiver", "received","total")).count(), is(1.0));
        assertThat(counter(buildName("test", "messageReceiver", "received","null")).count(), is(1.0));
    }

    @Test
    void messageReceived() {
        sut.messageReceived("destination1");

        assertThat(counter(buildName("test", "messageReceiver", "received","total")).count(), is(1.0));
        assertThat(counter(buildName("test", "messageReceiver", "received","destination1")).count(), is(1.0));
    }

    @Test
    void subscriptionCreated() {
        sut = new MetricsCollector("test2");
        sut.subscriptionCreated("destination2");

        assertThat(meterRegistry.get(buildName("test2", "messageReceiver", "subscriptions", "total")).gauge().value(), is(1.0));
        assertThat(meterRegistry.get(buildName("test2", "messageReceiver", "subscriptions", "destination2")).gauge().value(), is(1.0));
    }

    @Test
    void subscriptionCreatedCanBeInvokedWithNull()  {
        sut = new MetricsCollector("test3");
        sut.subscriptionCreated(null);

        assertThat(meterRegistry.get(buildName("test3", "messageReceiver", "subscriptions", "total")).gauge().value(), is(1.0));
        assertThat(meterRegistry.get(buildName("test3", "messageReceiver", "subscriptions", "null")).gauge().value(), is(1.0));
    }

    @Test
    void subscriptionDestroyedForNonExistentSubscription()  {
        sut.subscriptionDestroyed("destination3");

        assertThat(meterRegistry.get(buildName("test", "messageReceiver", "subscriptions", "total")).gauge().value(), is(-1.0));
    }

    @Test
    void subscriptionDestroyedCanBeInvokedWithNull() {
        sut.subscriptionDestroyed(null);
        assertThat(meterRegistry.get(buildName("test", "messageReceiver", "subscriptions", "total")).gauge().value(), is(-1.0));
    }

}