package com.dotc.nova.metrics;

import static com.codahale.metrics.MetricRegistry.*;

import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.*;

public class Metrics {
	private final MetricRegistry metricRegistry = new MetricRegistry();
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

	public <T extends Metric> void register(T metric, Class clazz, String... idPath) {
		metricRegistry.register(name(clazz, idPath), metric);
	}

	public void remove(Class clazz, String... idPath) {
		metricRegistry.remove(name(clazz, idPath));
	}

	public Meter getMeter(Class clazz, String... idPath) {
		return metricRegistry.meter(name(clazz, idPath));
	}

	public Counter getCounter(Class clazz, String... idPath) {
		return metricRegistry.counter(MetricRegistry.name(clazz, idPath));
	}

	public Timer getTimer(Class clazz, String... idPath) {
		return metricRegistry.timer(MetricRegistry.name(clazz, idPath));
	}

	public Histogram getHistogramm(Class clazz, String... idPath) {
		return metricRegistry.histogram(MetricRegistry.name(clazz, idPath));
	}
}
