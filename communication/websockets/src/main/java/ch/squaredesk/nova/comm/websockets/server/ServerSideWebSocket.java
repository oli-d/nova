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

package ch.squaredesk.nova.comm.websockets.server;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocketAdapter;
import org.glassfish.grizzly.websockets.WebSocketListener;

public class ServerSideWebSocket extends WebSocket {
    private final org.glassfish.grizzly.websockets.WebSocket underlyingWebSocket;
    private final WebSocketListener listener;

    ServerSideWebSocket(String destination,
                               org.glassfish.grizzly.websockets.WebSocket underlyingWebSocket,
                               MessageTranscriber<String> messageTranscriber,
                               MetricsCollector metricsCollector) {
        super(destination, messageTranscriber, metricsCollector);
        this.underlyingWebSocket = underlyingWebSocket;

        this.listener = new WebSocketAdapter() {
            @Override
            public void onClose(org.glassfish.grizzly.websockets.WebSocket socket, DataFrame frame) {
                propagateCloseEvent(CloseReason.GOING_AWAY);
            }

            @Override
            public void onMessage(org.glassfish.grizzly.websockets.WebSocket socket, String text) {
                propagateNewMessage(text);
                /**
                 * One comment regarding backpressure: if the stream subscribers are too slow,
                 * backpressure will be applied here. So if e.g. the subscriber need one hour
                 * to process a message, the next onNext() call will be blocked for that hour.
                 * But be aware that the next message has already been read from the wire into
                 * memory, so if the process is killed at this point in time the message that
                 * was not yet read from the wire is lost.
                 * TL;DR: backpressure is applied, but it does not prevent from loss of messages
                 */
            }
        };
        underlyingWebSocket.add(listener);
    }

    @Override
    public void doSend(String message) {
        underlyingWebSocket.send(message);
    }


    @Override
    protected void doClose(CloseReason closeReason) {
        underlyingWebSocket.remove(listener);
        underlyingWebSocket.close(closeReason.code, closeReason.text);
    }

    @Override
    protected boolean isOpen() {
        return underlyingWebSocket.isConnected();
    }

}
