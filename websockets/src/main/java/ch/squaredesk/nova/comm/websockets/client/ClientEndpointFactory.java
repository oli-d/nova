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
import com.ning.http.client.ws.WebSocketUpgradeHandler;
import io.reactivex.functions.Function;

import java.util.function.Consumer;

public class ClientEndpointFactory {
    private final MessageTranscriber<String> messageTranscriber;

    public ClientEndpointFactory(MessageTranscriber<String> messageTranscriber) {
        this.messageTranscriber = messageTranscriber;
    }

    public ClientEndpoint createFor (
            AsyncHttpClient httpClient,
            String destination,
            MetricsCollector metricsCollector)  {

        StreamCreatingWebSocketTextListener listener = new StreamCreatingWebSocketTextListener();
        WebSocketUpgradeHandler webSocketUpgradeHandler =new WebSocketUpgradeHandler.Builder()
                .addWebSocketListener(listener)
                .build();
        com.ning.http.client.ws.WebSocket underlyingWebSocket = openConnection(httpClient, destination, webSocketUpgradeHandler);

        WebSocket webSocket = createWebSocket(destination, underlyingWebSocket, metricsCollector);
        Function<com.ning.http.client.ws.WebSocket, WebSocket> webSocketFactory = rawSocket -> webSocket;

        EndpointStreamSource endpointStreamSource =
                EndpointStreamSourceFactory.createStreamSourceFor(destination, webSocketFactory, listener, metricsCollector);

        Consumer<CloseReason> closeAction = closeReason -> {
            if (underlyingWebSocket instanceof GrizzlyWebSocketAdapter) {
                GrizzlyWebSocketAdapter gwsa = (GrizzlyWebSocketAdapter) underlyingWebSocket;
                gwsa.getGrizzlyWebSocket().close(closeReason.code, closeReason.text);
            } else {
                underlyingWebSocket.close();
            }
            listener.close();
        };
        return new ClientEndpoint(destination, endpointStreamSource, webSocket, closeAction, messageTranscriber, metricsCollector);
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

    private WebSocket createWebSocket(
            String destination,
            com.ning.http.client.ws.WebSocket webSocket,
            MetricsCollector metricsCollector) {

        SendAction sendAction = new SendAction() {
            @Override
            public <T> void accept(T message) throws Exception {
                String messageAsString = messageTranscriber.getOutgoingMessageTranscriber(message).apply(message);
                webSocket.sendMessage(messageAsString);
                metricsCollector.messageSent(destination);
            }
        };
        Runnable closeAction = () -> metricsCollector.subscriptionDestroyed(destination);

        return new WebSocket(sendAction, closeAction);
    }

    private static <MessageType> MessageType unmarshal (
            String destination,
            String message,
            Function<String, MessageType> messageUnmarshaller,
            MetricsCollector metricsCollector) {
        try {
            return messageUnmarshaller.apply(message);
        } catch (Exception e) {
            metricsCollector.unparsableMessageReceived(destination);
            throw new RuntimeException("Unable to unmarshal incoming message " + message + " on destination " + destination, e);
        }
    }
}
