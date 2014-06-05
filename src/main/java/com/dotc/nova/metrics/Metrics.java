package com.dotc.nova.metrics;

import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;

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

	public void remove(String... idPath) {
		metricRegistry.remove(name(idPath));
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
}
