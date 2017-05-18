package ch.squaredesk.nova.metrics;

import com.codahale.metrics.Metric;

import java.util.Map;

public interface CompoundMetric extends Metric {
    Map<String, Object> getValues();
}
