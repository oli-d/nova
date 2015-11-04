package com.dotc.nova.events.metrics;

public class NoopRunnableTimer extends ExecutionTimeMeasurer {
    public NoopRunnableTimer() {
        super(null, null);
    }

    @Override
    public void monitorRuntimeIfEnabled(Object identifier, Runnable codeBlockToMeasure) {
        codeBlockToMeasure.run();
    }
}
