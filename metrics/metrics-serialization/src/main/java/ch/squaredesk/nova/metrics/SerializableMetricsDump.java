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
    public String hostName;
    public String hostAddress;
    public Map<String, Map<String, Object>> metrics;

    public SerializableMetricsDump() {
    }

    public static SerializableMetricsDump createFor (MetricsDump metricsDump) {
        SerializableMetricsDump dump = new SerializableMetricsDump();
        dump.timestamp = metricsDump.timestamp;
        dump.hostAddress = metricsDump.hostAddress;
        dump.hostName = metricsDump.hostName;
        dump.metrics = MetricsConverter.convert(metricsDump.metrics);
        return dump;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerializableMetricsDump that = (SerializableMetricsDump) o;
        return timestamp == that.timestamp &&
                Objects.equals(hostName, that.hostName) &&
                Objects.equals(hostAddress, that.hostAddress) &&
                Objects.equals(metrics, that.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, hostName, hostAddress, metrics);
    }
}
