/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.sending;

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
    private MetricsCollector sut = new MetricsCollector("test");
    private SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setup() {
        Metrics.globalRegistry.add(meterRegistry);
    }

    @AfterEach
    void destroy() {
        Metrics.globalRegistry.remove(meterRegistry);
    }


    @Test
    void messageSentCanBeInvokedWithNull() {
        sut.messageSent(null);

        assertThat(counter(buildName("test", "messageSender", "sent", "total")).count(), is(1.0));
        assertThat(counter(buildName("test", "messageSender", "sent", "null")).count(), is(1.0));
    }

    @Test
    void messageSending() {
        sut.messageSent("destination1");

        assertThat(counter(buildName("test","messageSender", "sent", "total")).count(), is(1.0));
        assertThat(counter(buildName("test", "messageSender", "sent", "destination1")).count(), is(1.0));
    }

}