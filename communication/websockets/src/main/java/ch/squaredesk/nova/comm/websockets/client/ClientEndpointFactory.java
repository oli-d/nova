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
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.providers.grizzly.websocket.GrizzlyWebSocketAdapter;
import com.ning.http.client.ws.WebSocketListener;
import com.ning.http.client.ws.WebSocketUpgradeHandler;
import io.reactivex.Single;

import java.util.function.Consumer;

public class ClientEndpointFactory {
    private final MessageTranscriber<String> messageTranscriber;

    public ClientEndpointFactory(MessageTranscriber<String> messageTranscriber) {
        this.messageTranscriber = messageTranscriber;
    }

    public WebSocket createFor (
            AsyncHttpClient httpClient,
            String destination,
            MetricsCollector metricsCollector)  {

        StreamCreatingWebSocketTextListener listener = new StreamCreatingWebSocketTextListener();
        WebSocketUpgradeHandler webSocketUpgradeHandler =new WebSocketUpgradeHandler.Builder()
                .addWebSocketListener(listener)
                .build();
        com.ning.http.client.ws.WebSocket underlyingWebSocket = openConnection(httpClient, destination, webSocketUpgradeHandler);

        SendAction sendAction = new SendAction() {
            @Override
            public <T> void accept(T message) throws Exception {
                String messageAsString = messageTranscriber.getOutgoingMessageTranscriber(message).apply(message);
                underlyingWebSocket.sendMessage(messageAsString);
                metricsCollector.messageSent(destination);
            }
        };
        Consumer<CloseReason> closeAction = closeReason -> {
            metricsCollector.subscriptionDestroyed(destination);
            if (underlyingWebSocket instanceof GrizzlyWebSocketAdapter) {
                GrizzlyWebSocketAdapter gwsa = (GrizzlyWebSocketAdapter) underlyingWebSocket;
                gwsa.getGrizzlyWebSocket().close(closeReason.code, closeReason.text);
            } else {
                underlyingWebSocket.close();
            }
            listener.close();
        };

        Single<Long> closeEventSingle = Single.create(s ->
            underlyingWebSocket.addWebSocketListener(new WebSocketListener() {
                @Override
                public void onOpen(com.ning.http.client.ws.WebSocket websocket) {
                }

                @Override
                public void onClose(com.ning.http.client.ws.WebSocket websocket) {
                    s.onSuccess(System.currentTimeMillis());
                }

                @Override
                public void onError(Throwable t) {
                }
            }
        ));

        return new WebSocket(sendAction, closeAction, underlyingWebSocket::isOpen, closeEventSingle);
    }

    private static com.ning.http.client.ws.WebSocket openConnection(
            AsyncHttpClient httpClient,
            String destination,
            WebSocketUpgradeHandler webSocketUpgradeHandler) {
        try {
            return httpClient.prepareGet(destination).execute(webSocketUpgradeHandler).get();
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to " + destination, e);
        }
    }
}
