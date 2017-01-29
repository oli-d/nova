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

	public <T extends Metric> void register(T metric, String... idPath) {
		metricRegistry.register(name(idPath), metric);
	}

	public boolean remove(String... idPath) {
		return metricRegistry.remove(name(idPath));
	}

	public Meter getMeter(String... idPath) {
		return metricRegistry.meter(name(idPath));
	}

	public Counter getCounter(String... idPath) {
		return metricRegistry.counter(name(idPath));
	}

	public Timer getTimer(String... idPath) {
		return metricRegistry.timer(name(idPath));
	}

	public Histogram getHistogram(String... idPath) {
		return metricRegistry.histogram(name(idPath));
	}

	public SettableGauge getGauge(String... idPath) {
        SortedMap<String, Gauge> gauges = metricRegistry.getGauges((metricName, metric) -> name(idPath).equals(metricName));
        if (gauges.isEmpty()) {
            return metricRegistry.register(name(idPath), new SettableGauge());
        }
        return (SettableGauge) gauges.get(name(idPath));
	}

	public Map<String, Metric> getMetrics() {
		return metricRegistry.getMetrics();
	}

	private String name(String... idPath) {
		int count = idPath.length;
		StringBuilder sb = new StringBuilder();
		int idx = 0;
		for (String s : idPath) {
			sb.append(s);
			if (idx < count - 1) {
				sb.append('.');
			}
			idx++;
		}
		return sb.toString();
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
