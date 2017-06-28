/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova;

import ch.squaredesk.nova.metrics.GarbageCollectionMeter;
import ch.squaredesk.nova.metrics.MemoryMeter;
import ch.squaredesk.nova.metrics.MetricsDump;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NovaTest {
    @Test
    void jvmMetricsAreCapturedByDefault() throws Exception {
        Nova sut = Nova.builder().build();

        MetricsDump metricsDump = sut.metrics.dump();
        assertTrue(metricsDump.metrics.get("jvm.mem") instanceof MemoryMeter);
        assertTrue(metricsDump.metrics.get("jvm.gc") instanceof GarbageCollectionMeter);
        // there might be environments that don't support CPU metrics, so we don't test it here
    }

    @Test
    void jvmMetricsCapturingCanBeSwitchedOff() throws Exception {
        Nova sut = Nova.builder().captureJvmMetrics(false).build();

        MetricsDump metricsDump = sut.metrics.dump();
        assertNull(metricsDump.metrics.get("jvm.mem"));
        assertNull(metricsDump.metrics.get("jvm.gc"));
    }

}
