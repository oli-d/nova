package ch.squaredesk.nova.metrics;

import ch.squaredesk.nova.Nova;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MetricsDumpToMapConverterTest {

    @Test
    void nullDumpThrows() {
        assertThrows(NullPointerException.class, () -> MetricsConverter.convert(null));
    }

    @Test
    void nullAdditionalAttributesIsOk() {
        Nova nova = Nova.builder().build();
        assertNotNull(MetricsConverter.convert(nova.metrics.dump().metrics, null));
    }

    @Test
    void metricsDumpConvertedAsExpected() {
        Nova nova = Nova.builder().build();
        MetricsDump dump = nova.metrics.dump();

        Map<String, Map<String, Object>> dumpAsMap = MetricsConverter.convert(dump.metrics);

        dump.metrics.keySet().forEach(key -> assertTrue(dumpAsMap.containsKey(key.toString())));
        assertThat(dumpAsMap.size(), is(dump.metrics.size() ));
        dump.metrics.keySet().forEach(metricName -> {
            String key = metricName.toString();
            assertTrue(dumpAsMap.containsKey(key));
            assertTrue(dumpAsMap.get(key).containsKey("type"));
        });
    }

    @Test
    void additionalAttributesUsedForEveryMetric() {
        Nova nova = Nova.builder().build();
        MetricsDump dump = nova.metrics.dump();
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put("key1", "value1");
        additionalAttributes.put("key2", "value2");

        Map<String, Map<String, Object>> dumpAsMap = MetricsConverter.convert(dump.metrics, additionalAttributes);

        dumpAsMap.entrySet().stream()
                .filter(entry -> dump.metrics.keySet().contains(entry.getKey()))
                .forEach(entry -> {
                    assertThat(((Map) entry.getValue()).get("key1"), is("value1"));
                    assertThat(((Map) entry.getValue()).get("key2"), is("value2"));
                });
    }
}