package com.dotc.nova.events.metrics;

import com.dotc.nova.metrics.Metrics;

import java.util.concurrent.atomic.AtomicBoolean;

public class ExecutionTimeMeasurer extends MetricsCollector {

    private final AtomicBoolean monitorRuntime = new AtomicBoolean(false);

    public ExecutionTimeMeasurer(Metrics metrics, String identifierPrefix) {
        super(metrics, "EventHandler".equalsIgnoreCase(identifierPrefix) ? identifierPrefix :
                "EventHandler." + identifierPrefix);
    }

    
    public void setMonitorRuntime(boolean monitorRuntime) {
        this.monitorRuntime.set(monitorRuntime);
    }

    public void setTrackingEnabled(boolean enabled, Object event) {
        super.setTrackingEnabled(enabled, String.valueOf(event));
    }


    public void monitorRuntimeIfEnabled(Object event, Runnable codeBlockToMeasure) {
        String identifierString = String.valueOf(event);
        if (monitorRuntime.get() || shouldBeTracked(identifierString)) {
            long start = System.nanoTime();
            codeBlockToMeasure.run();
            long differenceInNanos = System.nanoTime() - start;
            getGauge("runtime", identifierString).setValue(differenceInNanos);
        } else {
            codeBlockToMeasure.run();
        }
    }

}
