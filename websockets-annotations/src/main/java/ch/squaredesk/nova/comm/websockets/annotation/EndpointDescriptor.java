/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */
package ch.squaredesk.nova.comm.websockets.annotation;

import io.reactivex.BackpressureStrategy;

import java.lang.reflect.Method;

class EndpointDescriptor {
    final Object objectToInvokeMethodOn;
    final Method methodToInvoke;
    final String destination;
    final boolean captureTimings;
    final BackpressureStrategy backpressureStrategy;

    EndpointDescriptor(
            Object objectToInvokeMethodOn,
            Method methodToInvoke,
            String destination,
            boolean captureMetrics,
            BackpressureStrategy backpressureStrategy) {
        this.objectToInvokeMethodOn = objectToInvokeMethodOn;
        this.methodToInvoke = methodToInvoke;
        this.destination = destination;
        this.captureTimings = captureMetrics;
        this.backpressureStrategy = backpressureStrategy;
    }
}
