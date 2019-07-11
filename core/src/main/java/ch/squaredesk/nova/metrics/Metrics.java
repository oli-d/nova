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

import ch.squaredesk.nova.tuples.Pair;
import io.dropwizard.metrics5.*;
import io.dropwizard.metrics5.Timer;
import io.reactivex.Observable;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Metrics {
    public final MetricRegistry metricRegistry = new MetricRegistry();
    private Slf4jReporter logReporter;

    public MetricsDump dump() {
        return dump(Collections.emptyList());
    }

    public MetricsDump dump(List<Pair<String, String>> additionalInfo) {
        return new MetricsDump(metricRegistry.getMetrics(), additionalInfo);
    }

    /**
     * Returns an observable that continuously emits all registered metrics. The passed parameters define the
     * interval between two dumps.
     */
    public Observable<MetricsDump> dumpContinuously(long interval, TimeUnit timeUnit) {
        return dumpContinuously(interval, timeUnit, Collections.emptyList());
    }

    /**
     * Returns an observable that continuously emits all registered metrics. The passed parameters define the
     * interval between two dumps. The passed additionalInfo will be added on every dump
     */
    public Observable<MetricsDump> dumpContinuously(long interval, TimeUnit timeUnit, List<Pair<String, String>> additionalInfo) {
        if (interval <= 0) {
            throw new IllegalArgumentException("interval must be greater than 0");
        }
        Objects.requireNonNull(timeUnit, "timeUnit must not be null");

        return Observable
                .interval(interval, interval, timeUnit)
                .map(count -> dump(additionalInfo));
    }


    public void dumpContinuouslyToLog(long dumpInterval, TimeUnit timeUnit) {
        if (logReporter == null) {
            logReporter = Slf4jReporter.forRegistry(metricRegistry).outputTo(LoggerFactory.getLogger(Metrics.class))
                    .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
        } else {
            logReporter.close();
        }
        logReporter.start(dumpInterval, timeUnit);
    }

    public void dumpToLog() {
        if (logReporter == null) {
            logReporter = Slf4jReporter.forRegistry(metricRegistry).outputTo(LoggerFactory.getLogger(Metrics.class))
                    .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
        }
        logReporter.report();
    }

    public <T extends Metric> void register(MetricName metricName, T metric) {
        metricRegistry.register(metricName, metric);
    }

    public <T extends Metric> void register(T metric, String idPathFirst, String... idPathRemainder) {
        register(name(idPathFirst,idPathRemainder), metric);
    }

    public boolean remove(MetricName metricName) {
        return metricRegistry.remove(metricName);
    }

    public boolean remove(String idPathFirst, String... idPathRemainder) {
        return remove(name(idPathFirst,idPathRemainder));
    }

    public Meter getMeter(MetricName metricName) {
        return metricRegistry.meter(metricName);
    }

    public Meter getMeter(String idPathFirst, String... idPathRemainder) {
        return getMeter(name(idPathFirst,idPathRemainder));
    }

    public Counter getCounter(MetricName metricName) {
        return metricRegistry.counter(metricName);
    }

    public Counter getCounter(String idPathFirst, String... idPathRemainder) {
        return getCounter(name(idPathFirst,idPathRemainder));
    }

    public Timer getTimer(MetricName metricName) {
        return metricRegistry.timer(metricName);
    }

    public Timer getTimer(String idPathFirst, String... idPathRemainder) {
        return getTimer(name(idPathFirst,idPathRemainder));
    }

    public Histogram getHistogram(MetricName metricName) {
        return metricRegistry.histogram(metricName);
    }

    public Histogram getHistogram(String idPathFirst, String... idPathRemainder) {
        return getHistogram(name(idPathFirst,idPathRemainder));
    }

    public Gauge getGauge(MetricName metricName) {
        Optional<Map.Entry<MetricName, Gauge>> gaugeEntry = metricRegistry
                .getGauges()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals(metricName))
                .findFirst();

        if (gaugeEntry.isPresent()) {
            return gaugeEntry.get().getValue();
        } else {
            return metricRegistry.register(metricName, new SettableGauge());
        }
    }

    public Gauge getGauge(String idPathFirst, String... idPathRemainder) {
        return getGauge(name(idPathFirst, idPathRemainder));
    }

    public Map<MetricName, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }

    public static MetricName name(String idPathFirst, String... idPathRemainder) {
        return MetricRegistry.name(idPathFirst, idPathRemainder);
    }

}
