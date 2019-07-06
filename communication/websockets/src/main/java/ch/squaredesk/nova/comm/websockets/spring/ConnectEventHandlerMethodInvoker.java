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

import ch.squaredesk.nova.comm.websockets.WebSocket;
import ch.squaredesk.nova.metrics.Metrics;
import io.dropwizard.metrics5.Timer;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ConnectEventHandlerMethodInvoker {
    private final static Logger LOGGER = LoggerFactory.getLogger(ConnectEventHandlerMethodInvoker.class);

    static Consumer<WebSocket> createFor(EventHandlerEndpointDescriptor endpointDescriptor,
                                                            String adapterIdentifier,
                                                            Metrics metrics) {


        Consumer<WebSocket> consumer = webSocket -> {
            if (endpointDescriptor.logInvocations) {
                LOGGER.debug("Invoking connect event handler {}.{} for socket {}",
                        endpointDescriptor.objectToInvokeMethodOn.getClass().getSimpleName(),
                        endpointDescriptor.methodToInvoke.getName(),
                        webSocket
                );
            }
            try {
                endpointDescriptor.methodToInvoke.invoke(
                        endpointDescriptor.objectToInvokeMethodOn,
                        webSocket);
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
