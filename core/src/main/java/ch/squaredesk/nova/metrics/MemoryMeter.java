/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.regex.Pattern;

import static ch.squaredesk.nova.metrics.Metrics.name;

/**
 * This class is heavily inspired by the MemoryUsageGaugeSet of the dropwizards metrics-jvm package (dependency:
 * io.dropwizard.metrics:metrics-jvm). We just want to have all values in one big object, instead of many, many separate
 * Metric objects.
 */
public class MemoryMeter implements CompoundMetric {
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private final MemoryMXBean mxBean;
    private final List<MemoryPoolMXBean> memoryPools;

    public MemoryMeter() {
        this(ManagementFactory.getMemoryMXBean(), ManagementFactory.getMemoryPoolMXBeans());
    }

    public MemoryMeter(MemoryMXBean mxBean, Collection<MemoryPoolMXBean> memoryPools) {
        this.mxBean = mxBean;
        this.memoryPools = new ArrayList<>(memoryPools);
    }

    @Override
    public Map<String, Object> getValues() {
        MemoryUsage nonHeapUsage = mxBean.getNonHeapMemoryUsage();
        MemoryUsage heapUsage = mxBean.getHeapMemoryUsage();
        Map<String, Object> values = new HashMap<>();

        values.put(name("heapInitial"), heapUsage.getInit());
        values.put(name("heapUsed"), heapUsage.getUsed());
        values.put(name("heapMax"), heapUsage.getMax());
        values.put(name("heapCommitted"), heapUsage.getCommitted());
        values.put(name("heapFree"), heapUsage.getMax() - heapUsage.getUsed());
        values.put(name("heapUsageInPercent"), (double)heapUsage.getUsed() / (double)heapUsage.getMax() * 100.0);
        values.put(name("totalInitial"), heapUsage.getInit() + nonHeapUsage.getInit());
        values.put(name("totalUsed"), heapUsage.getUsed() + nonHeapUsage.getUsed());
        values.put(name("totalMax"), heapUsage.getMax() + nonHeapUsage.getMax());
        values.put(name("totalCommitted"), heapUsage.getCommitted() + nonHeapUsage.getCommitted());
        values.put(name("totalFree"), heapUsage.getMax() + nonHeapUsage.getMax() - heapUsage.getUsed() - nonHeapUsage.getUsed());
        values.put(name("totalUsageInPercent"), ((double)heapUsage.getUsed() + (double)nonHeapUsage.getUsed()) /
                ((double)heapUsage.getMax() + (double)nonHeapUsage.getMax()) * 100.0);

        for (MemoryPoolMXBean pool : memoryPools) {
            String poolName = name("pools", WHITESPACE.matcher(pool.getName()).replaceAll("-")).toString();
            MemoryUsage usage = pool.getUsage();

            values.put(name(poolName, "usageInPercent"),
                    (double)usage.getUsed() / (usage.getMax() == -1 ? usage.getCommitted() : usage.getMax()) * 100.0);
            values.put(name(poolName, "init"), usage.getMax());
            values.put(name(poolName, "max"), usage.getMax());
            values.put(name(poolName, "max"), usage.getUsed());
            values.put(name(poolName, "committed"),usage.getUsed());
            // Only register GC usage metrics if the memory pool supports usage statistics.
            MemoryUsage collectionUsage = pool.getCollectionUsage();
            if (collectionUsage != null) {
                values.put(name(poolName, "used-after-gc"), collectionUsage.getUsed());
            }
        }

        return values;
    }
}
