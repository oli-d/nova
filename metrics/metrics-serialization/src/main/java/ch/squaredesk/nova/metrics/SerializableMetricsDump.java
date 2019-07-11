/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.metrics;

import java.util.Map;
import java.util.Objects;

public class SerializableMetricsDump {
    public long timestamp;
    public Map<String, Map<String, Object>> metrics;

    public SerializableMetricsDump() {
    }

    public static SerializableMetricsDump createFor (MetricsDump metricsDump) {
        SerializableMetricsDump dump = new SerializableMetricsDump();
        dump.timestamp = metricsDump.timestamp;
        dump.metrics = MetricsConverter.convert(metricsDump);
        return dump;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerializableMetricsDump that = (SerializableMetricsDump) o;
        return timestamp == that.timestamp &&
                Objects.equals(metrics, that.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, metrics);
    }
}
