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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.lmax.disruptor.RingBuffer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class RingBufferMetricSet implements MetricSet {
    private final RingBuffer ringBuffer;

    public RingBufferMetricSet(RingBuffer ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();
        gauges.put(name("ringbuffer", "size" ), (Gauge<Integer>) ringBuffer::getBufferSize);
        gauges.put(name("ringbuffer", "used"), (Gauge<Long>) () -> ringBuffer.getBufferSize() -
                ringBuffer.remainingCapacity());
        return Collections.unmodifiableMap(gauges);
    }
}
