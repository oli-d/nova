/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class WebSocket<MessageType> {
    private final Consumer<MessageType> sendAction;
    private final Runnable closeAction;

    public WebSocket(Consumer<MessageType> sendAction, Runnable closeAction) {
        Objects.requireNonNull(sendAction, "sendAction must not be null");
        Objects.requireNonNull(closeAction, "closeAction must not be null");
        this.sendAction = sendAction;
        this.closeAction = closeAction;
    }

    public final void send(MessageType message) {
        sendAction.accept(message);
    }

    public final void close() {
        closeAction.run();
    }

}
