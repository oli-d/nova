/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.metrics;

import ch.squaredesk.nova.tuples.Pair;
import com.codahale.metrics.Metric;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MetricsDump {
    public final long timestamp;
    public final Map<String, Metric> metrics;
    public final List<Pair<String, String>> additionalInfo;

    public MetricsDump(Map<String, Metric> metrics) {
        this(metrics, Collections.emptyList());
    }

    public MetricsDump(Map<String, Metric> metrics, Pair<String, String>... additionalInfo) {
        this(metrics, Arrays.asList(additionalInfo));
    }

    public MetricsDump(Map<String, Metric> metrics, List<Pair<String, String>> additionalInfo) {
        this.timestamp = System.currentTimeMillis();

        this.metrics = metrics;
        this.additionalInfo = additionalInfo;
    }
}
