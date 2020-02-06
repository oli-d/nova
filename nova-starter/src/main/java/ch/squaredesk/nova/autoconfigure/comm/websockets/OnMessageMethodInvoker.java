/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.websockets;

import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.websockets.IncomingMessageMetaData;
import ch.squaredesk.nova.metrics.Metrics;
import com.codahale.metrics.Timer;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class OnMessageMethodInvoker {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnMessageMethodInvoker.class);

    private OnMessageMethodInvoker() {
    }

    static <MessageType> Consumer<IncomingMessage<MessageType, IncomingMessageMetaData>> createFor(OnMessageHandlerEndpointDescriptor endpointDescriptor,
                                                                                                   String adapterIdentifier,
                                                                                                   Metrics metrics) {

        Consumer<IncomingMessage<MessageType, IncomingMessageMetaData>> consumer = incomingMessage -> {
            if (endpointDescriptor.logInvocations) {
                LOGGER.debug("Invoking message event handler {}.{} with meta data {}",
                        endpointDescriptor.objectToInvokeMethodOn.getClass().getSimpleName(),
                        endpointDescriptor.methodToInvoke.getName(),
                        incomingMessage.metaData
                );
            }
            try {
                endpointDescriptor.methodToInvoke.invoke(
                        endpointDescriptor.objectToInvokeMethodOn,
                        incomingMessage.message,
                        incomingMessage.metaData.details.webSocket);
            } catch (Exception e) {
                LOGGER.error("Unable to invoke handler for message {}", incomingMessage, e);
            }
        };

        if (endpointDescriptor.captureTimings) {
            Timer timer = metrics.getTimer(adapterIdentifier, "invocationTime",
                    endpointDescriptor.objectToInvokeMethodOn.getClass().getSimpleName(),
                    endpointDescriptor.methodToInvoke.getName());
            return incoming -> {
                Timer.Context context = timer.time();
                try {
                    consumer.accept(incoming);
                } finally {
                    context.stop();
                }
            };
        } else {
            return consumer;
        }
    }
}
