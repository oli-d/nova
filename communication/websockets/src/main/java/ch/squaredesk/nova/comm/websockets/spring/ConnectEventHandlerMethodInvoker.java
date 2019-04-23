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

import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;


class ConnectEventHandlerMethodInvoker implements Consumer<WebSocket> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ConnectEventHandlerMethodInvoker.class);
    private final Object objectToInvokeMethodOn;
    private final Method methodToInvoke;
    private final MetricsCollector metricsCollector;

    private ConnectEventHandlerMethodInvoker(Object objectToInvokeMethodOn, Method methodToInvoke, MetricsCollector metricsCollector) {
        this.objectToInvokeMethodOn = objectToInvokeMethodOn;
        this.methodToInvoke = methodToInvoke;
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void accept(WebSocket webSocket) {
        // FIXME: timing metrics
        try {
            methodToInvoke.invoke(objectToInvokeMethodOn, webSocket);
        } catch (Exception e) {
            LOGGER.error("Unable to invoke web socket event handler {} ", methodToInvoke.getName(), e);
        }
    }

    static ConnectEventHandlerMethodInvoker createFor(EventHandlerEndpointDescriptor endpointDescriptor, MetricsCollector metricsCollector) {
        return new ConnectEventHandlerMethodInvoker(endpointDescriptor.objectToInvokeMethodOn,
                endpointDescriptor.methodToInvoke,
                endpointDescriptor.captureTimings ? metricsCollector : null);
    }
}
