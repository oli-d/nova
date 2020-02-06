/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.core.events;

import io.reactivex.BackpressureStrategy;

import java.lang.reflect.Method;

public class EventHandlerDescription {
    public final Object bean;
    public final Method methodToInvoke;
    public final String[] events;
    public final BackpressureStrategy backpressureStrategy;
    public final boolean captureInvocationTimeMetrics;

    public EventHandlerDescription(Object bean, 
                                   Method methodToInvoke, 
                                   String[] events, 
                                   BackpressureStrategy backpressureStrategy, 
                                   boolean captureInvocationTimeMetrics) {
        this.bean = bean;
        this.methodToInvoke = methodToInvoke;
        this.events = events;
        this.backpressureStrategy = backpressureStrategy;
        this.captureInvocationTimeMetrics = captureInvocationTimeMetrics;
    }
}
