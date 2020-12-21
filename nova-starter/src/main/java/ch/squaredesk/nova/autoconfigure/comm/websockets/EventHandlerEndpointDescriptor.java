/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.autoconfigure.comm.websockets;

import java.lang.reflect.Method;

class EventHandlerEndpointDescriptor {
    final Object objectToInvokeMethodOn;
    final Method methodToInvoke;
    final String destination;
    final boolean captureTimings;
    final boolean logInvocations;

    EventHandlerEndpointDescriptor(
            Object objectToInvokeMethodOn,
            Method methodToInvoke,
            String destination,
            boolean captureMetrics,
            boolean logInvocations) {
        this.objectToInvokeMethodOn = objectToInvokeMethodOn;
        this.methodToInvoke = methodToInvoke;
        this.destination = destination;
        this.captureTimings = captureMetrics;
        this.logInvocations = logInvocations;
    }
}
