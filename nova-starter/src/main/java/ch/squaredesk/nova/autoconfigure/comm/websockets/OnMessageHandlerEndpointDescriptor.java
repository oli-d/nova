/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.autoconfigure.comm.websockets;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.functions.Function;

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
