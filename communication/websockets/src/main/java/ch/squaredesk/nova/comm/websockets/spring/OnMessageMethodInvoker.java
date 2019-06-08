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

package ch.squaredesk.nova.comm.websockets.spring;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.websockets.IncomingMessageMetaData;
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;


class OnMessageMethodInvoker<MessageType> implements Consumer<IncomingMessage<MessageType, IncomingMessageMetaData>> {
    private final static Logger LOGGER = LoggerFactory.getLogger(OnMessageMethodInvoker.class);
    private final Object objectToInvokeMethodOn;
    private final Method methodToInvoke;
    private final MetricsCollector metricsCollector;

    private OnMessageMethodInvoker(Object objectToInvokeMethodOn, Method methodToInvoke, MetricsCollector metricsCollector) {
        this.objectToInvokeMethodOn = objectToInvokeMethodOn;
        this.methodToInvoke = methodToInvoke;
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void accept(IncomingMessage<MessageType, IncomingMessageMetaData> message) {
        // FIXME: timing metrics
        try {
            methodToInvoke.invoke(objectToInvokeMethodOn, message.message, message.metaData.details.webSocket);
        } catch (Exception e) {
            LOGGER.error("Unable to invoke handler for message {}", message, e);
        }
    }

    static <MessageType> OnMessageMethodInvoker<MessageType> createFor(OnMessageHandlerEndpointDescriptor endpointDescriptor, MetricsCollector metricsCollector) {
        return new OnMessageMethodInvoker<>(endpointDescriptor.objectToInvokeMethodOn,
                endpointDescriptor.methodToInvoke,
                endpointDescriptor.captureTimings ? metricsCollector : null);
    }
}