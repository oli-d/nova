/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets.client;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.websockets.*;

import java.util.function.Consumer;

public class ClientEndpoint extends Endpoint {
    private final WebSocket webSocket;

    public ClientEndpoint(String destination,
                          EndpointStreamSource endpointStreamSource,
                          WebSocket webSocket,
                          Consumer<CloseReason> closeAction,
                          MessageTranscriber<String> messageTranscriber,
                          MetricsCollector metricsCollector) {
        super(destination, endpointStreamSource, closeAction, messageTranscriber, metricsCollector);
        this.webSocket = webSocket;
    }

    public <T> void send(T message) throws Exception {
        webSocket.send(message);
    }

    public String getUserProperty(String propertyId) {
        return getUserProperty(propertyId, String.class);
    }
    public <PropertyType> PropertyType getUserProperty(String propertyId, Class<PropertyType> propertyType) {
        return webSocket.getUserProperty(propertyId, propertyType);
    }

    public void setUserProperty(String propertyId, Object value) {
        webSocket.setUserProperty(propertyId, value);
    }

    public void clearUserProperties() {
        webSocket.clearUserProperties();
    }
}
