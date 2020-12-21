/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CpuMeter implements CompoundMetric {
    private static final Logger logger = LoggerFactory.getLogger(CpuMeter.class);

    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final Method systemCpuLoadRetrievalMethod;
    private final Method processCpuLoadRetrievalMethod;

    public CpuMeter() {
        Method[] availableMethods = operatingSystemMXBean.getClass().getDeclaredMethods();
        systemCpuLoadRetrievalMethod = Arrays.stream(availableMethods)
                .filter(m -> "getSystemCpuLoad".equals(m.getName()))
                .filter(m -> {
                    try {
                        m.setAccessible(true);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);
        processCpuLoadRetrievalMethod = Arrays.stream(availableMethods)
                .filter(m -> "getProcessCpuLoad".equals(m.getName()))
                .filter(m -> {
                    try {
                        m.setAccessible(true);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);
    }

    public boolean environmentSupportsCpuMetrics() {
        return systemCpuLoadRetrievalMethod != null || processCpuLoadRetrievalMethod != null;
    }

    @Override
    public Map<String, Object> getValues() {
        Map<String, Object> values = new HashMap<>();
        if (systemCpuLoadRetrievalMethod!=null) {
            values.put(Metrics.name("systemLoadPercent"), invoke(systemCpuLoadRetrievalMethod) * 100.0);
        }
        if (processCpuLoadRetrievalMethod!=null) {
            values.put(Metrics.name("processLoadPercent"), invoke(processCpuLoadRetrievalMethod) * 100.0);
        }
        return values;
    }

    private double invoke (Method m) {
        try {
            return (double) m.invoke(operatingSystemMXBean);
        } catch (Exception e) {
            logger.error("Failed to invoke method {}", m.getName(), e);
            return -1;
        }
    }
}
