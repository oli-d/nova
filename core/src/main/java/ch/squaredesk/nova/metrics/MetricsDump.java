package ch.squaredesk.nova.metrics;

import com.codahale.metrics.*;

import java.net.InetAddress;
import java.util.Map;

public class MetricsDump {
    private static InetAddress myInetAddress;

    public final long timestamp;
    public final String hostName;
    public final String hostAddress;
    public final Map<String, Metric> metrics;

    public MetricsDump(Map<String, Metric> metrics) {
        this.metrics = metrics;
        this.timestamp = System.currentTimeMillis();

        if (myInetAddress == null) {
            try {
                myInetAddress = InetAddress.getLocalHost();
            } catch (Exception ex) {
                // swallow
            }
        }

        if (myInetAddress == null) {
            this.hostName = "n/a";
            this.hostAddress = "n/a";
        } else {
            this.hostName = myInetAddress.getHostName();
            this.hostAddress = myInetAddress.getHostAddress();
        }
    }
}
