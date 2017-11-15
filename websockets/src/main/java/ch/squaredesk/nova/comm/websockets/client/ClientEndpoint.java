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

import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.Endpoint;
import ch.squaredesk.nova.comm.websockets.EndpointStreamSource;
import ch.squaredesk.nova.comm.websockets.WebSocket;

import java.util.function.Consumer;

public class ClientEndpoint<MessageType> extends Endpoint<MessageType> {
    private final WebSocket<MessageType> webSocket;

    public ClientEndpoint(EndpointStreamSource<MessageType> endpointStreamSource,
                          WebSocket<MessageType> webSocket,
                          Consumer<CloseReason> closeAction) {
        super(endpointStreamSource, closeAction);
        this.webSocket = webSocket;
    }

    public void send(MessageType message) {
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
