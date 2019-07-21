/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics;

import ch.squaredesk.nova.tuples.Pair;
import io.dropwizard.metrics5.Metric;
import io.dropwizard.metrics5.MetricName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MetricsDump {
    public final long timestamp;
    public final Map<MetricName, Metric> metrics;
    public final List<Pair<String, String>> additionalInfo;

    public MetricsDump(Map<MetricName, Metric> metrics) {
        this(metrics, Collections.emptyList());
    }

    public MetricsDump(Map<MetricName, Metric> metrics, Pair<String, String>... additionalInfo) {
        this(metrics, Arrays.asList(additionalInfo));
    }

    public MetricsDump(Map<MetricName, Metric> metrics, List<Pair<String, String>> additionalInfo) {
        this.timestamp = System.currentTimeMillis();

        this.metrics = metrics;
        this.additionalInfo = additionalInfo;
    }
}
