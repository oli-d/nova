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

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.websockets.IncomingMessageMetaData;
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;


class MethodInvoker<MessageType> implements Consumer<IncomingMessage<MessageType, IncomingMessageMetaData<MessageType>>> {
    private final static Logger LOGGER = LoggerFactory.getLogger(MethodInvoker.class);
    private final Object objectToInvokeMethodOn;
    private final Method methodToInvoke;
    private final MetricsCollector metricsCollector;

    private MethodInvoker(Object objectToInvokeMethodOn, Method methodToInvoke, MetricsCollector metricsCollector) {
        this.objectToInvokeMethodOn = objectToInvokeMethodOn;
        this.methodToInvoke = methodToInvoke;
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void accept(IncomingMessage<MessageType, IncomingMessageMetaData<MessageType>> message) throws Exception {
        // FIXME: timing metrics
        try {
            methodToInvoke.invoke(objectToInvokeMethodOn, message.message, message.metaData.details.webSocket);
        } catch (Exception e) {
            LOGGER.error("Unable to invoke handler for message {}", message, e);
        }
    }

    static <MessageType> MethodInvoker<MessageType> createFor(EndpointDescriptor endpointDescriptor, MetricsCollector metricsCollector) {
        return new MethodInvoker<>(endpointDescriptor.objectToInvokeMethodOn,
                endpointDescriptor.methodToInvoke,
                endpointDescriptor.captureTimings ? metricsCollector : null);
    }
}
