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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This class is heavily inspired by the GarbageCollectorMetricSet of the dropwizards metrics-jvm package (dependency:
 * io.dropwizard.metrics:metrics-jvm). We just want to have all values in one big object, instead of many, many separate
 * Metric objects.
 *
 * Also, we currently skip MemoryPool stats
 */
public class GarbageCollectionMeter implements CompoundMetric {
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private final List<GarbageCollectorMXBean> garbageCollectors;

    /**
     * Creates a new set of gauges for all discoverable garbage collectors.
     */
    public GarbageCollectionMeter() {
        this(ManagementFactory.getGarbageCollectorMXBeans());
    }

    /**
     * Creates a new set of gauges for the given collection of garbage collectors.
     *
     * @param garbageCollectors    the garbage collectors
     */
    public GarbageCollectionMeter(Collection<GarbageCollectorMXBean> garbageCollectors) {
        this.garbageCollectors = new ArrayList<GarbageCollectorMXBean>(garbageCollectors);
    }

    @Override
    public Map<String, Object> getValues() {
        final Map<String, Object> values = new HashMap<>();
        for (final GarbageCollectorMXBean gc : garbageCollectors) {
            final String name = WHITESPACE.matcher(gc.getName()).replaceAll("-");
            values.put(name(name, "count"), gc.getCollectionCount());
            values.put(name(name, "time"), gc.getCollectionTime());
        }
        return values;
    }

}
