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
import ch.squaredesk.nova.metrics.SerializableMetricsDump;
import io.reactivex.functions.Consumer;

public class KafkaMetricsReporter implements Consumer<MetricsDump> {
    private final KafkaAdapter<SerializableMetricsDump> kafkaAdapter;
    private final String topicName;

    public KafkaMetricsReporter(KafkaAdapter<SerializableMetricsDump> kafkaAdapter, String topicName) {
        this.kafkaAdapter = kafkaAdapter;
        this.topicName = topicName;
    }

    @Override
    public void accept(MetricsDump metricsDump) {
        kafkaAdapter.sendMessage(topicName, SerializableMetricsDump.createFor(metricsDump)).subscribe();
    }

}
