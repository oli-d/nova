/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.websockets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class CloseEventHandlerMethodInvoker {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloseEventHandlerMethodInvoker.class);

    private CloseEventHandlerMethodInvoker() {
    }

//    static Consumer<Pair<WebSocket, CloseReason>> createFor(EventHandlerEndpointDescriptor endpointDescriptor,
//                                                            String adapterIdentifier,
//                                                            Metrics metrics) {
//
//
//        Consumer<Pair<WebSocket, CloseReason>> consumer = webSocketCloseReasonPair -> {
//            if (endpointDescriptor.logInvocations) {
//                LOGGER.debug("Invoking close event handler {}.{} for socket {} and close reason {}",
//                        endpointDescriptor.objectToInvokeMethodOn.getClass().getSimpleName(),
//                        endpointDescriptor.methodToInvoke.getName(),
//                        webSocketCloseReasonPair.item1(),
//                        webSocketCloseReasonPair.item2()
//                );
//            }
//
//            try {
//                endpointDescriptor.methodToInvoke.invoke(
//                        endpointDescriptor.objectToInvokeMethodOn,
//                        webSocketCloseReasonPair.item1(),
//                        webSocketCloseReasonPair.item2());
//            } catch (Exception e) {
//                LOGGER.error("Unable to invoke web socket event handler {} ", endpointDescriptor.methodToInvoke.getName(), e);
//            }
//        };
//
//        if (endpointDescriptor.captureTimings) {
//            Timer timer = metrics.getTimer(adapterIdentifier, "invocationTime",
//                    endpointDescriptor.objectToInvokeMethodOn.getClass().getSimpleName(),
//                    endpointDescriptor.methodToInvoke.getName());
//            return incoming -> {
//                Timer.Context context = timer.time();
//                try {
//                    consumer.accept(incoming);
//                } finally {
//                    context.stop();
//                }
//            };
//        } else {
//            return consumer;
//        }
//    }
}
