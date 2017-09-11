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
        assertThrows(NullPointerException.class, () -> MetricsDumpToMapConverter.convert(null));
    }

    @Test
    void nullAdditionalAttributesIsOk() {
        Nova nova = Nova.builder().build();
        MetricsDump dump = nova.metrics.dump();
        assertNotNull(MetricsDumpToMapConverter.convert(dump, null));
    }

    @Test
    void metricsDumpConvertedAsExpected() {
        Nova nova = Nova.builder().build();
        MetricsDump dump = nova.metrics.dump();

        Map<String, Object> dumpAsMap = MetricsDumpToMapConverter.convert(dump);

        dump.metrics.keySet().forEach(key -> assertTrue(dumpAsMap.containsKey(key)));
        assertThat(dumpAsMap.size(), is(dump.metrics.size() + 3));
        assertTrue(dumpAsMap.containsKey("timestamp"));
        assertTrue(dumpAsMap.containsKey("hostName"));
        assertTrue(dumpAsMap.containsKey("hostAddress"));
        dumpAsMap.entrySet().stream()
                .filter(entry -> dump.metrics.containsKey(entry.getKey()))
                .forEach(entry -> assertTrue(((Map) entry.getValue()).containsKey("type")));
    }

    @Test
    void additionalAttributesUsedForEveryMetric() {
        Nova nova = Nova.builder().build();
        MetricsDump dump = nova.metrics.dump();
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put("key1", "value1");
        additionalAttributes.put("key2", "value2");

        Map<String, Object> dumpAsMap = MetricsDumpToMapConverter.convert(dump, additionalAttributes);

        dumpAsMap.entrySet().stream()
                .filter(entry -> dump.metrics.keySet().contains(entry.getKey()))
                .forEach(entry -> {
                    assertThat(((Map) entry.getValue()).get("key1"), is("value1"));
                    assertThat(((Map) entry.getValue()).get("key2"), is("value2"));
                });
    }
}