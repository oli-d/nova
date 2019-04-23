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

import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import ch.squaredesk.nova.tuples.Pair;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;


class CloseEventHandlerMethodInvoker implements Consumer<Pair<WebSocket, CloseReason>> {
    private final static Logger LOGGER = LoggerFactory.getLogger(CloseEventHandlerMethodInvoker.class);
    private final Object objectToInvokeMethodOn;
    private final Method methodToInvoke;
    private final MetricsCollector metricsCollector;

    private CloseEventHandlerMethodInvoker(Object objectToInvokeMethodOn, Method methodToInvoke, MetricsCollector metricsCollector) {
        this.objectToInvokeMethodOn = objectToInvokeMethodOn;
        this.methodToInvoke = methodToInvoke;
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void accept(Pair<WebSocket, CloseReason> webSocketCloseReasonPair) {
        // FIXME: timing metrics
        try {
            methodToInvoke.invoke(objectToInvokeMethodOn, webSocketCloseReasonPair._1, webSocketCloseReasonPair._2);
        } catch (Exception e) {
            LOGGER.error("Unable to invoke web socket event handler {} ", methodToInvoke.getName(), e);
        }
    }

    static CloseEventHandlerMethodInvoker createFor(EventHandlerEndpointDescriptor endpointDescriptor, MetricsCollector metricsCollector) {
        return new CloseEventHandlerMethodInvoker(endpointDescriptor.objectToInvokeMethodOn,
                endpointDescriptor.methodToInvoke,
                endpointDescriptor.captureTimings ? metricsCollector : null);
    }
}
