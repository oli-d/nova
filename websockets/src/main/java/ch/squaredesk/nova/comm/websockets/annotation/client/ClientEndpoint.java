/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets.annotation.client;

import ch.squaredesk.nova.comm.websockets.annotation.CloseReason;
import ch.squaredesk.nova.comm.websockets.annotation.Endpoint;
import ch.squaredesk.nova.comm.websockets.annotation.EndpointStreamSource;
import ch.squaredesk.nova.comm.websockets.annotation.WebSocket;

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
}
