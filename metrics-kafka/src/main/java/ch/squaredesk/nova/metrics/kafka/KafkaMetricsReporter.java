/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics.kafka;

import ch.squaredesk.nova.comm.kafka.KafkaAdapter;
import ch.squaredesk.nova.metrics.MetricsDump;
import ch.squaredesk.nova.metrics.MetricsDumpToMapConverter;
import io.reactivex.functions.Consumer;

import java.util.Collections;
import java.util.Map;

public class KafkaMetricsReporter implements Consumer<MetricsDump> {
    private final KafkaAdapter<Map> kafkaAdapter;
    private final String topicName;
    private final Map<String, Object> additionalMetricAttributes;

    public KafkaMetricsReporter(KafkaAdapter<Map> kafkaAdapter, String topicName) {
        this(kafkaAdapter, topicName, Collections.EMPTY_MAP);
    }

    public KafkaMetricsReporter(KafkaAdapter<Map> kafkaAdapter, String topicName, Map<String, Object> additionalMetricAttributes) {
        this.kafkaAdapter = kafkaAdapter;
        this.topicName = topicName;
        this.additionalMetricAttributes = additionalMetricAttributes;
    }

    @Override
    public void accept(MetricsDump metricsDump) throws Exception {
        Map<String, Object> dumpAsMap = MetricsDumpToMapConverter.convert(metricsDump);
        kafkaAdapter.sendMessage(topicName, dumpAsMap).subscribe();
    }

}
