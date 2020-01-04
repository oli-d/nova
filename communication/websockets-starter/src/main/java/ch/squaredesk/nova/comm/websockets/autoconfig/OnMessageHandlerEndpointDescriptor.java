/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */
package ch.squaredesk.nova.comm.websockets.autoconfig;

import io.reactivex.BackpressureStrategy;
import io.reactivex.functions.Function;

import java.lang.reflect.Method;

class OnMessageHandlerEndpointDescriptor extends EventHandlerEndpointDescriptor {
    final Class<?> messageType;
    final Function<?, String> marshaller;
    final Function<String, ?> unmarshaller;
    final BackpressureStrategy backpressureStrategy;

    OnMessageHandlerEndpointDescriptor(
            Object objectToInvokeMethodOn,
            Method methodToInvoke,
            String destination,
            Class<?> messageType,
            Function<?, String> marshaller,
            Function<String, ?> unmarshaller,
            boolean captureMetrics,
            boolean logInvocations,
            BackpressureStrategy backpressureStrategy) {
        super(objectToInvokeMethodOn, methodToInvoke, destination, captureMetrics, logInvocations);
        this.messageType = messageType;
        this.marshaller = marshaller;
        this.unmarshaller = unmarshaller;
        this.backpressureStrategy = backpressureStrategy;
    }
}
