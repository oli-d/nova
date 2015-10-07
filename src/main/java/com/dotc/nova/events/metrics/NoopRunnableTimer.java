package com.dotc.nova.events.metrics;

import com.dotc.nova.metrics.Metrics;

public class NoopRunnableTimer extends RunnableTimer {
    public NoopRunnableTimer() {
        super(null, null);
    }

    @Override
    public void monitorRuntimeIfEnabled(Object identifier, Runnable codeBlockToMeasure) {
        codeBlockToMeasure.run();
    }
}
