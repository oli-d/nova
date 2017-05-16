package ch.squaredesk.nova.service;

import com.codahale.metrics.*;

import java.util.Map;

public class ServiceMetricsSet {
    public final long timestamp;
    public final String serviceName;
    public final String instanceId;
    public final Map<String, Gauge> gauges;
    public final Map<String, Counter> counters;
    public final Map<String, Histogram> histograms;
    public final Map<String, Meter> meters;
    public final Map<String, Timer> timers;

    public ServiceMetricsSet(String serviceName,
                             String instanceId,
                             Map<String, Gauge> gauges,
                             Map<String, Counter> counters,
                             Map<String, Histogram> histograms,
                             Map<String, Meter> meters,
                             Map<String, Timer> timers) {
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.timestamp = System.currentTimeMillis();
        this.gauges = gauges;
        this.counters = counters;
        this.histograms = histograms;
        this.meters = meters;
        this.timers = timers;
    }
}
