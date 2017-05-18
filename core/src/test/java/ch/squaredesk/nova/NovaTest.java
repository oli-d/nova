package ch.squaredesk.nova;

import ch.squaredesk.nova.metrics.MetricsDump;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NovaTest {
    @Test
    void jvmMetricsAreCapturedByDefault() throws Exception {
        Nova sut = Nova.builder().build();

        MetricsDump metricsDump = sut.metrics.dump();
        assertNotNull(metricsDump.memoryMeter);
        assertNotNull(metricsDump.garbageCollectionMeter);
        // there might be environments that don't support CPU metrics, so we don't test it here
    }

    @Test
    void jvmMetricsCapturingCanBeSwitchedOff() throws Exception {
        Nova sut = Nova.builder().captureJvmMetrics(false).build();

        MetricsDump metricsDump = sut.metrics.dump();
        assertNull(metricsDump.memoryMeter);
        assertNull(metricsDump.garbageCollectionMeter);
        // there might be environments that don't support CPU metrics, so we don't test it here
    }

}
