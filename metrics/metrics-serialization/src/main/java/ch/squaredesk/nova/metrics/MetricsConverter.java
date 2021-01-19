/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics;

import ch.squaredesk.nova.tuples.Pair;
import ch.squaredesk.nova.tuples.Tuple3;
import com.codahale.metrics.Metric;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * This class takes a dump, generated by Nova's Metrics instance and converts them into a map
 * that can easily be serialized and deserialized.
 *
 * The resulting map contains one entry per metric in the dump, each can be looked up by the Metric name. Additionally,
 * for each metric entry we add the additionalInfo stored in the dump.
 *
 * Every Metric itself is again represented as a Map, one MapEntry per attribute. Additionally, we add
 *   - type
 *
 */
public class MetricsConverter {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private MetricsConverter() {
    }

    public static Map<String, Map<String, Object>> convert(MetricsDump dump) {
        HashMap<String, Map<String, Object>> returnValue = new HashMap<>();

        dump.metrics.entrySet().stream()
                // for each metric, create a tuple containing the type, the name and the metric itself
                .map(entry -> new Tuple3<>(
                        entry.getValue().getClass().getSimpleName(),
                        entry.getKey(),
                        entry.getValue()))
                // convert each metric to a Map and enrich with our default values, as well as the
                // (eventually) passed additionalAttributes
                .map(tupleTypeAndNameAndMetric -> {
                    Map<String, Object> map = toMap(tupleTypeAndNameAndMetric._3);
                    map.put("type", tupleTypeAndNameAndMetric.item1());
                    if (dump.additionalInfo != null) {
                        dump.additionalInfo.forEach(pair -> map.put(pair.item1(), pair.item2()));
                    }
                    return new Pair<>(tupleTypeAndNameAndMetric.item2(), map);
                })
                // and add it to the return value
                .forEach(metricNameMapPair -> returnValue.put(metricNameMapPair.item1(), metricNameMapPair.item2()));

        return returnValue;
    }

    private static Map<String, Object> toMap (CompoundMetric compoundMetric) {
        Map<String, Object> returnValue = new HashMap<>();
        Map<String, Object> values = compoundMetric.getValues();

        if (!values.isEmpty()) {
            values.forEach(returnValue::put);
        }
        returnValue.put("type", compoundMetric.getClass().getSimpleName());

        return returnValue;
    }

    private static Map<String, Object> toMap(Metric metric) {
        if (metric instanceof CompoundMetric) {
            return toMap((CompoundMetric) metric);
        } else {
            Map map = objectMapper.convertValue(metric, Map.class);
            map.put("type", metric.getClass().getSimpleName());
            return map;
        }
    }

}
