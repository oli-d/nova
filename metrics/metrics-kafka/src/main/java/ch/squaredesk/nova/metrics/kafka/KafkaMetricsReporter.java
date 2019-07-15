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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaMetricsReporter implements Consumer<MetricsDump> {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMetricsReporter.class);

    private final KafkaAdapter kafkaAdapter;
    private final String topicName;

    public KafkaMetricsReporter(KafkaAdapter kafkaAdapter, String topicName) {
        this.kafkaAdapter = kafkaAdapter;
        this.topicName = topicName;
    }

    @Override
    public void accept(MetricsDump metricsDump) {
        try {
            kafkaAdapter.sendMessage(topicName, SerializableMetricsDump.createFor(metricsDump)).subscribe(
                    metaData -> logger.trace("Successfully sent message {}", metaData),
                    error -> logger.error("Unable to send MetricsDump to topic {}", topicName, error)
            );
        } catch (Exception e) {
            logger.error("Unable to send metrics dump", e);
        }
    }

}
