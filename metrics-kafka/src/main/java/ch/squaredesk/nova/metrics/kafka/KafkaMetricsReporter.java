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

import ch.squaredesk.nova.comm.kafka.KafkaCommAdapter;
import ch.squaredesk.nova.metrics.CompoundMetric;
import ch.squaredesk.nova.metrics.MetricsDump;
import ch.squaredesk.nova.metrics.MetricsDumpToMapConverter;
import ch.squaredesk.nova.tuples.Pair;
import ch.squaredesk.nova.tuples.Tuple3;
import com.codahale.metrics.Metric;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KafkaMetricsReporter implements Consumer<MetricsDump> {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMetricsReporter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ZoneId zoneForTimestamps = ZoneId.of("UTC");
    private final KafkaCommAdapter<Object> kafkaCommAdapter;
    private final String topicName;
    private final Map<String, Object> additionalMetricAttributes;

    public KafkaMetricsReporter(KafkaCommAdapter<Object> kafkaCommAdapter, String topicName) {
        this(kafkaCommAdapter, topicName, Collections.EMPTY_MAP);
    }

    public KafkaMetricsReporter(KafkaCommAdapter<Object> kafkaCommAdapter, String topicName, Map<String, Object> additionalMetricAttributes) {
        this.kafkaCommAdapter = kafkaCommAdapter;
        this.topicName = topicName;
        this.additionalMetricAttributes = additionalMetricAttributes;
    }

    @Override
    public void accept(MetricsDump metricsDump) throws Exception {
        Map<String, Object> dumpAsMap = MetricsDumpToMapConverter.convert(metricsDump);
        kafkaCommAdapter.sendMessage(topicName, dumpAsMap).subscribe();
    }

}
