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

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.websockets.*;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ws.WebSocketUpgradeHandler;

import java.util.function.Consumer;
import java.util.function.Function;

public class ClientEndpointFactory {

    public static <MessageType> ClientEndpoint<MessageType> createFor (
            AsyncHttpClient httpClient,
            String destination,
            MessageMarshaller<MessageType, String> messageMarshaller,
            MessageUnmarshaller<String, MessageType> messageUnmarshaller,
            MetricsCollector metricsCollector)  {
        StreamCreatingWebSocketTextListener<MessageType> listener =
                new StreamCreatingWebSocketTextListener<>(text -> unmarshal(destination, text, messageUnmarshaller, metricsCollector));
        WebSocketUpgradeHandler webSocketUpgradeHandler =new WebSocketUpgradeHandler.Builder()
                .addWebSocketListener(listener)
                .build();
        com.ning.http.client.ws.WebSocket underlyingWebSocket = openConnection(httpClient, destination, webSocketUpgradeHandler);

        WebSocket<MessageType> webSocket = createWebSocket(destination, underlyingWebSocket, messageMarshaller, metricsCollector);
        Function<com.ning.http.client.ws.WebSocket, WebSocket<MessageType>> webSocketFactory = rawSocket -> webSocket;

        EndpointStreamSource<MessageType> endpointStreamSource =
                EndpointStreamSourceFactory.createStreamSourceFor(destination, webSocketFactory, listener, metricsCollector);

        Consumer<CloseReason> closeAction = closeReason -> {
            underlyingWebSocket.close();
            listener.close();
        };
        return new ClientEndpoint<>(endpointStreamSource, webSocket, closeAction);
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

    private static <MessageType> WebSocket<MessageType> createWebSocket(
            String destination,
            com.ning.http.client.ws.WebSocket webSocket,
            MessageMarshaller<MessageType, String> messageMarshaller,
            MetricsCollector metricsCollector) {

        return new WebSocket<>(
                message -> {
                    String messageAsString = marshal(message, messageMarshaller);
                    webSocket.sendMessage(messageAsString);
                    metricsCollector.messageSent(destination);
                },
                () -> {
                    metricsCollector.subscriptionDestroyed(destination);
                });
    }

    private static <MessageType> String marshal (MessageType message, MessageMarshaller<MessageType, String> messageMarshaller) {
        try {
            return messageMarshaller.marshal(message);
        } catch (Exception e) {
            // TODO: metric?
            throw new RuntimeException("Unable to marshal message " + message, e);
        }
    }

    private static <MessageType> MessageType unmarshal (
            String destination,
            String message,
            MessageUnmarshaller<String, MessageType> messageUnmarshaller,
            MetricsCollector metricsCollector) {
        try {
            return messageUnmarshaller.unmarshal(message);
        } catch (Exception e) {
            metricsCollector.unparsableMessageReceived(destination);
            throw new RuntimeException("Unable to unmarshal incoming message " + message + " on destination " + destination, e);
        }
    }
}
