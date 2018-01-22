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

import com.codahale.metrics.*;
import io.reactivex.Observable;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Metrics {
    public final MetricRegistry metricRegistry = new MetricRegistry();
    private Slf4jReporter logReporter;

    public MetricsDump dump() {
        return new MetricsDump(metricRegistry.getMetrics());
    }

    /**
     * Returns an observable that continuously dumps all registered metrics. The passed parameters define the
     * interval between two dumps.
     */
    public Observable<MetricsDump> dumpContinuously(long interval, TimeUnit timeUnit) {
        if (interval <= 0) throw new IllegalArgumentException("interval must be greater than 0");
        Objects.requireNonNull(timeUnit, "timeUnit must not be null");

        return Observable.interval(interval, interval, timeUnit)
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

    public Gauge getGauge(String idPathFirst, String... idPathRemainder) {
        String theMetricName = name(idPathFirst, idPathRemainder);
        Optional<Map.Entry<String, Gauge>> gaugeEntry = metricRegistry
                .getGauges()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals(theMetricName))
                .findFirst();

        if (gaugeEntry.isPresent()) {
            return gaugeEntry.get().getValue();
        } else {
            return metricRegistry.register(name(idPathFirst,idPathRemainder), new SettableGauge());
        }
    }

    public Map<String, Metric> getMetrics() {
        return metricRegistry.getMetrics();
    }

    public static String name(String idPathFirst, String... idPathRemainder) {
        return MetricRegistry.name(idPathFirst, idPathRemainder);
    }

}
