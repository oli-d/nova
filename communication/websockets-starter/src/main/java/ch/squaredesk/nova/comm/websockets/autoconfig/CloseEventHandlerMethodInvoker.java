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

import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.tuples.Pair;
import com.codahale.metrics.Timer;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class CloseEventHandlerMethodInvoker {
    private final static Logger LOGGER = LoggerFactory.getLogger(CloseEventHandlerMethodInvoker.class);

    static Consumer<Pair<WebSocket, CloseReason>> createFor(EventHandlerEndpointDescriptor endpointDescriptor,
                                                    String adapterIdentifier,
                                                    Metrics metrics) {


        Consumer<Pair<WebSocket, CloseReason>> consumer = webSocketCloseReasonPair -> {
            if (endpointDescriptor.logInvocations) {
                LOGGER.debug("Invoking close event handler {}.{} for socket {} and close reason {}",
                        endpointDescriptor.objectToInvokeMethodOn.getClass().getSimpleName(),
                        endpointDescriptor.methodToInvoke.getName(),
                        webSocketCloseReasonPair._1,
                        webSocketCloseReasonPair._2
                );
            }

            try {
                endpointDescriptor.methodToInvoke.invoke(
                        endpointDescriptor.objectToInvokeMethodOn,
                        webSocketCloseReasonPair._1,
                        webSocketCloseReasonPair._2);
            } catch (Exception e) {
                LOGGER.error("Unable to invoke web socket event handler {} ", endpointDescriptor.methodToInvoke.getName(), e);
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
