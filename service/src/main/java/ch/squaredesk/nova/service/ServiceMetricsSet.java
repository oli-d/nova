package ch.squaredesk.nova.service;

import com.codahale.metrics.*;

import java.net.InetAddress;
import java.util.Map;

public class ServiceMetricsSet {
    private static InetAddress myInetAddress;

    public final long timestamp;
    public final String serverName;
    public final String serverAddress;
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

        if (myInetAddress == null) {
            try {
                myInetAddress = InetAddress.getLocalHost();
            } catch (Exception ex) {
                // swallow
            }
        }

        if (myInetAddress == null) {
            this.serverName = "n/a";
            this.serverAddress = "n/a";
        } else {
            this.serverName = myInetAddress.getHostName();
            this.serverAddress = myInetAddress.getHostAddress();
        }
    }
}
