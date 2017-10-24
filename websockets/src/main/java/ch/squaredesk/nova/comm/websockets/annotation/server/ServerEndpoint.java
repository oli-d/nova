/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets.annotation.server;

import ch.squaredesk.nova.comm.websockets.annotation.CloseReason;
import ch.squaredesk.nova.comm.websockets.annotation.Endpoint;
import ch.squaredesk.nova.comm.websockets.annotation.EndpointStreamSource;

import java.util.function.Consumer;

public class ServerEndpoint<MessageType> extends Endpoint<MessageType> {
    private final Consumer<MessageType> broadcastAction;

    public ServerEndpoint(EndpointStreamSource<MessageType> endpointStreamSource,
                          Consumer<MessageType> broadcastAction,
                          Consumer<CloseReason> closeAction) {
        super(endpointStreamSource, closeAction);
        this.broadcastAction = broadcastAction;
    }

    public void broadcast (MessageType message) {
        broadcastAction.accept(message);
    }
}
