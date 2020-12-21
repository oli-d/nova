/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.websockets;

import ch.squaredesk.nova.comm.websockets.WebSocket;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.tuples.Pair;
import com.codahale.metrics.Timer;
import io.reactivex.rxjava3.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ErrorEventHandlerMethodInvoker  {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorEventHandlerMethodInvoker.class);

    private ErrorEventHandlerMethodInvoker() {
    }

    static Consumer<Pair<WebSocket, Throwable>> createFor(EventHandlerEndpointDescriptor endpointDescriptor,
                                                          String adapterIdentifier,
                                                          Metrics metrics) {


        Consumer<Pair<WebSocket, Throwable>> consumer = webSocketErrorPair -> {
            if (endpointDescriptor.logInvocations) {
                LOGGER.debug("Invoking error event handler {}.{} for socket {} with error {}",
                        endpointDescriptor.objectToInvokeMethodOn.getClass().getSimpleName(),
                        endpointDescriptor.methodToInvoke.getName(),
                        webSocketErrorPair._1,
                        webSocketErrorPair._2
                );
            }
            try {
                endpointDescriptor.methodToInvoke.invoke(
                        endpointDescriptor.objectToInvokeMethodOn,
                        webSocketErrorPair._1,
                        webSocketErrorPair._2);
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
