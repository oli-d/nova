package ch.squaredesk.nova.events.annotation;

import io.reactivex.BackpressureStrategy;

import java.lang.reflect.Method;

public class EventHandlerDescription {
    public final Object bean;
    public final Method methodToInvoke;
    public final String[] events;
    public final BackpressureStrategy backpressureStrategy;
    public final boolean dispatchOnBusinessLogicThread;
    public final boolean captureInvocationTimeMetrics;

    public EventHandlerDescription(Object bean, 
                                   Method methodToInvoke, 
                                   String[] events, 
                                   BackpressureStrategy backpressureStrategy, 
                                   boolean dispatchOnBusinessLogicThread, 
                                   boolean captureInvocationTimeMetrics) {
        this.bean = bean;
        this.methodToInvoke = methodToInvoke;
        this.events = events;
        this.backpressureStrategy = backpressureStrategy;
        this.dispatchOnBusinessLogicThread = dispatchOnBusinessLogicThread;
        this.captureInvocationTimeMetrics = captureInvocationTimeMetrics;
    }
}
