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

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.codahale.metrics.*;
import org.slf4j.LoggerFactory;

public class Metrics {
    public final MetricRegistry metricRegistry = new MetricRegistry();
    private Slf4jReporter logReporter;

    public void dumpContinuously(ScheduledReporter reporter, long dumpInterval, TimeUnit timeUnit) {
        reporter.start(dumpInterval, timeUnit);
    }

    public void dumpOnce(ScheduledReporter reporter) {
        reporter.report();
    }

    public void dumpContinuouslyToLog(long dumpInterval, TimeUnit timeUnit) {
        if (logReporter == null) {
            logReporter = Slf4jReporter.forRegistry(metricRegistry).outputTo(LoggerFactory.getLogger(Metrics.class))
                    .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
        } else {
            logReporter.close();
        }
        dumpContinuously(logReporter, dumpInterval, timeUnit);

    }

    public void dumpOnceToLog() {
        if (logReporter == null) {
            logReporter = Slf4jReporter.forRegistry(metricRegistry).outputTo(LoggerFactory.getLogger(Metrics.class))
                    .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
        }
        dumpOnce(logReporter);
    }

    public <T extends Metric> void register(T metric, String idPathFirst, String... idPathRemainder) {
        metricRegistry.register(name(idPathFirst,idPathRemainder), metric);
    }

    public boolean remove(String idPathFirst, String... idPathRemainder) {
        return metricRegistry.remove(name(idPathFirst,idPathRemainder));
    }

    public Meter getMeter(String idPathFirst, String... idPathRemainder) {
        return metricRegistry.meter(name(idPathFirst,idPathRemainder));
    }

    public Counter getCounter(String idPathFirst, String... idPathRemainder) {
        return metricRegistry.counter(name(idPathFirst,idPathRemainder));
    }

    public Timer getTimer(String idPathFirst, String... idPathRemainder) {
        return metricRegistry.timer(name(idPathFirst,idPathRemainder));
    }

    public Histogram getHistogram(String idPathFirst, String... idPathRemainder) {
        return metricRegistry.histogram(name(idPathFirst,idPathRemainder));
    }

    public SettableGauge getGauge(String idPathFirst, String... idPathRemainder) {
        SortedMap<String, Gauge> gauges = metricRegistry.getGauges((metricName, metric) -> name(idPathFirst,idPathRemainder).equals(metricName));
        if (gauges.isEmpty()) {
            return metricRegistry.register(name(idPathFirst,idPathRemainder), new SettableGauge());
        }
        return (SettableGauge) gauges.get(name(idPathFirst,idPathRemainder));
    }

    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }

    public static String name(String idPathFirst, String... idPathRemainder) {
        return MetricRegistry.name(idPathFirst, idPathRemainder);
    }

    public static class SettableGauge implements Gauge<Long> {
        private final AtomicLong value = new AtomicLong();
        @Override
        public Long getValue() {
            return value.get();
        }

        public void setValue(long value) {
            this.value.set(value);
        }
    }
}
