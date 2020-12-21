/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics;

import ch.squaredesk.nova.Nova;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class MetricsConverterTest {

    @Test
    void nullDumpThrows() {
        assertThrows(NullPointerException.class, () -> MetricsConverter.convert(null));
    }

    @Test
    void nullAdditionalAttributesIsOk() {
        Nova nova = Nova.builder().build();
        assertNotNull(MetricsConverter.convert(nova.metrics.dump()));
    }

    @Test
    void metricsDumpConvertedAsExpected() {
        Nova nova = Nova.builder().build();
        nova.metrics.addAdditionalInfoForDumps("key", "val");
        MetricsDump dump = nova.metrics.dump();

        Map<String, Map<String, Object>> dumpAsMap = MetricsConverter.convert(dump);

        dump.metrics.keySet().forEach(key -> assertTrue(dumpAsMap.containsKey(key.toString())));
        assertThat(dumpAsMap.size(), is(dump.metrics.size() ));
        dump.metrics.keySet().forEach(metricName -> {
            String key = metricName.toString();
            assertTrue(dumpAsMap.containsKey(key));
            assertTrue(dumpAsMap.get(key).containsKey("type"));
            assertThat(dumpAsMap.get(key).get("key"), is("val"));
        });
    }

    @Test
    void additionalAttributesUsedForEveryMetric() {
        Nova nova = Nova.builder().build();
        nova.metrics.addAdditionalInfoForDumps("key1", "value1");
        nova.metrics.addAdditionalInfoForDumps("key2", "value2");
        MetricsDump dump = nova.metrics.dump();

        Map<String, Map<String, Object>> dumpAsMap = MetricsConverter.convert(dump);

        dumpAsMap.entrySet().stream()
                .filter(entry -> dump.metrics.keySet().contains(entry.getKey()))
                .forEach(entry -> {
                    assertThat(entry.getValue().get("key1"), is("value1"));
                    assertThat(entry.getValue().get("key2"), is("value2"));
                });
    }
}