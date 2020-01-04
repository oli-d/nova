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

package ch.squaredesk.nova.comm.websockets.client;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.websockets.CloseReason;
import ch.squaredesk.nova.comm.websockets.MetricsCollector;
import ch.squaredesk.nova.comm.websockets.WebSocket;
import com.ning.http.client.providers.grizzly.websocket.GrizzlyWebSocketAdapter;
import com.ning.http.client.ws.WebSocketCloseCodeReasonListener;
import com.ning.http.client.ws.WebSocketListener;
import com.ning.http.client.ws.WebSocketTextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSideWebSocket extends WebSocket {
    private static final Logger logger = LoggerFactory.getLogger(ClientSideWebSocket.class);

    private final com.ning.http.client.ws.WebSocket underlyingWebSocket;
    private final MyWebSocketListener listener;

    ClientSideWebSocket(String destination,
                               com.ning.http.client.ws.WebSocket underlyingWebSocket,
                               MessageTranscriber<String> messageTranscriber,
                               MetricsCollector metricsCollector) {
        super(destination, messageTranscriber, metricsCollector);
        this.underlyingWebSocket = underlyingWebSocket;

        this.listener = new MyWebSocketListener();
        underlyingWebSocket.addWebSocketListener(listener);
    }

    @Override
    public void doSend(String message) {
        underlyingWebSocket.sendMessage(message);
    }

    @Override
    protected void doClose(CloseReason closeReason) {
        try {
            if (underlyingWebSocket instanceof GrizzlyWebSocketAdapter) {
                GrizzlyWebSocketAdapter gwsa = (GrizzlyWebSocketAdapter) underlyingWebSocket;
                gwsa.getGrizzlyWebSocket().close(closeReason.code, closeReason.text);
            } else {
                underlyingWebSocket.close();
            }
        } finally {
            underlyingWebSocket.removeWebSocketListener(listener);
        }
    }

    @Override
    protected boolean isOpen() {
        return underlyingWebSocket.isOpen();
    }

    private class MyWebSocketListener implements WebSocketTextListener, WebSocketListener, WebSocketCloseCodeReasonListener {
        @Override
        public void onMessage(String messageText) {
            propagateNewMessage(messageText);
        }

        @Override
        public void onOpen(com.ning.http.client.ws.WebSocket websocket) {
        }

        @Override
        public void onClose(com.ning.http.client.ws.WebSocket websocket) {
            // noop, this is handled in the specific onClose(WebSocket, code, reason)
        }

        @Override
        public void onError(Throwable t) {
            propagateError(t);
        }

        @Override
        public void onClose(com.ning.http.client.ws.WebSocket websocket, int code, String reason) {
            logger.trace("onClose() invoked with webSocket={}, code={}, reason={}.", websocket, code, reason);
            CloseReason closeReason;
            try {
                closeReason = CloseReason.forCloseCode(code);
            } catch (Exception e) {
                closeReason = CloseReason.NO_STATUS_CODE;
            }
            propagateCloseEvent(closeReason);
        }
    }
}
