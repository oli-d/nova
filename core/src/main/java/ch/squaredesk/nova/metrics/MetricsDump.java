/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics;

import io.dropwizard.metrics5.Metric;
import io.dropwizard.metrics5.MetricName;

import java.net.InetAddress;
import java.util.Map;

public class MetricsDump {
    private static InetAddress myInetAddress;

    static {
        try {
            myInetAddress = InetAddress.getLocalHost();
        } catch (Exception ex) {
            // swallow
        }
    }

    public final long timestamp;
    public final String hostName;
    public final String hostAddress;
    public final Map<MetricName, Metric> metrics;

    public MetricsDump(Map<MetricName, Metric> metrics) {
        this.timestamp = System.currentTimeMillis();

        if (myInetAddress == null) {
            this.hostName = "n/a";
            this.hostAddress = "n/a";
        } else {
            this.hostName = myInetAddress.getHostName();
            this.hostAddress = myInetAddress.getHostAddress();
        }
        this.metrics = metrics;
    }

}
