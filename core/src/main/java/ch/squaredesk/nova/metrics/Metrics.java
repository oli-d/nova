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

import ch.squaredesk.nova.tuples.Pair;
import com.codahale.metrics.*;
import io.reactivex.Flowable;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class Metrics {
    public final MetricRegistry metricRegistry = new MetricRegistry();
    private Slf4jReporter logReporter;
    private List<Pair<String, String>> additionalInfo = new CopyOnWriteArrayList<>();

    public MetricsDump dump() {
        return new MetricsDump(metricRegistry.getMetrics(), additionalInfo);
    }

    public void addAdditionalInfoForDumps (String key, String value) {
        Optional.ofNullable(key)
                .ifPresent(k -> additionalInfo.add(Pair.create(k, value)));
    }

    /**
     * Returns an observable that continuously emits all registered metrics. The passed parameters define the
     * interval between two dumps.
     */
    public Flowable<MetricsDump> dumpContinuously(long interval, TimeUnit timeUnit) {
        if (interval <= 0) {
            throw new IllegalArgumentException("interval must be greater than 0");
        }
        Objects.requireNonNull(timeUnit, "timeUnit must not be null");

        return Flowable
                .interval(interval, interval, timeUnit)
                .map(count -> dump());
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

    public <T extends Metric> void register(String metricName, T metric) {
        metricRegistry.register(metricName, metric);
    }

    public <T extends Metric> void register(T metric, String idPathFirst, String... idPathRemainder) {
        register(name(idPathFirst,idPathRemainder), metric);
    }

    public boolean remove(String metricName) {
        return metricRegistry.remove(metricName);
    }

    public boolean remove(String idPathFirst, String... idPathRemainder) {
        return remove(name(idPathFirst,idPathRemainder));
    }

    public Meter getMeter(String metricName) {
        return metricRegistry.meter(metricName);
    }

    public Meter getMeter(String idPathFirst, String... idPathRemainder) {
        return getMeter(name(idPathFirst,idPathRemainder));
    }

    public Counter getCounter(String metricName) {
        return metricRegistry.counter(metricName);
    }

    public Counter getCounter(String idPathFirst, String... idPathRemainder) {
        return getCounter(name(idPathFirst,idPathRemainder));
    }

    public Timer getTimer(String metricName) {
        return metricRegistry.timer(metricName);
    }

    public Timer getTimer(String idPathFirst, String... idPathRemainder) {
        return getTimer(name(idPathFirst,idPathRemainder));
    }

    public Histogram getHistogram(String metricName) {
        return metricRegistry.histogram(metricName);
    }

    public Histogram getHistogram(String idPathFirst, String... idPathRemainder) {
        return getHistogram(name(idPathFirst,idPathRemainder));
    }

    public Gauge getGauge(String metricName) {
        Optional<Map.Entry<String, Gauge>> gaugeEntry = metricRegistry
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

    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }

    public static String name(String idPathFirst, String... idPathRemainder) {
        return MetricRegistry.name(idPathFirst, idPathRemainder);
    }

}
