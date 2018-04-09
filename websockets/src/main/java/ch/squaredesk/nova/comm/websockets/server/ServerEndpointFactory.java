/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets.server;

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.websockets.*;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServerEndpointFactory {
    private static final Scheduler lifecycleEventScheduler = Schedulers.io();
    private final ConcurrentHashMap<org.glassfish.grizzly.websockets.WebSocket, WebSocket<?>> webSockets = new ConcurrentHashMap<>();

    private <MessageType> WebSocket<MessageType> instantiateNewWebSocket(
            org.glassfish.grizzly.websockets.WebSocket webSocket,
            MessageMarshaller<MessageType, String> messageMarshaller) {

            return new WebSocket<>(
                    message -> {
                        String messageAsString = marshal(message, messageMarshaller);
                        webSocket.send(messageAsString);
                    },
                    webSocket::close);
    }

    private <MessageType> WebSocket<MessageType> createWebSocket(
            org.glassfish.grizzly.websockets.WebSocket webSocket,
            MessageMarshaller<MessageType, String> messageMarshaller) {

            WebSocket<?> retVal = webSockets.computeIfAbsent(
                    webSocket,
                    key -> instantiateNewWebSocket(key, messageMarshaller));
            return (WebSocket<MessageType>) retVal;
    }

    private <MessageType> String marshal (MessageType message, MessageMarshaller<MessageType, String> messageMarshaller) {
        try {
            return messageMarshaller.marshal(message);
        } catch (Exception e) {
            // TODO: metric?
            throw new RuntimeException("Unable to marshal message " + message, e);
        }
    }

    private <MessageType> MessageType unmarshal (
            String destination,
            String message,
            MessageUnmarshaller<String, MessageType> messageUnmarshaller,
            MetricsCollector metricsCollector) {
        try {
            return messageUnmarshaller.unmarshal(message);
        } catch (Exception e) {
            if (metricsCollector != null) {
                metricsCollector.unparsableMessageReceived(destination);
            }
            throw new RuntimeException("Unable to unmarshal incoming message " + message + " on destination " + destination, e);
        }
    }

    public <MessageType> ServerEndpoint<MessageType> createFor(
            String destination,
            MessageMarshaller<MessageType, String> messageMarshaller,
            MessageUnmarshaller<String, MessageType> messageUnmarshaller,
            MetricsCollector metricsCollector)  {

        String destinationForSubscription = destination.startsWith("/") ? destination : "/" + destination;
        String destinationForMetrics = destination.startsWith("/") ? destination.substring(1) : destination;

        StreamCreatingWebSocketApplication<MessageType> app =
                new StreamCreatingWebSocketApplication<>(text -> unmarshal(destinationForMetrics, text, messageUnmarshaller, metricsCollector));
        WebSocketEngine.getEngine().register("", destinationForSubscription, app);

        Function<org.glassfish.grizzly.websockets.WebSocket, WebSocket<MessageType>> webSocketCreator =
                socket -> createWebSocket(socket, messageMarshaller);

        EndpointStreamSource<MessageType> endpointStreamSource =
                EndpointStreamSourceFactory.createStreamSourceFor(destinationForMetrics, webSocketCreator, app, metricsCollector);

        // register all connecting WebSockets
        Disposable subscriptionConnections = app.connectingSockets()
                .subscribeOn(lifecycleEventScheduler).subscribe(webSocketCreator::apply);
        // unregister all disconnecting WebSockets
        Disposable subscriptionDisconnections = app.closingSockets()
                .subscribeOn(lifecycleEventScheduler).subscribe(pair -> webSockets.remove(pair._1));
        Consumer<MessageType> broadcastAction = message -> {
            String messageAsString = marshal(message, messageMarshaller);

            Set<org.glassfish.grizzly.websockets.WebSocket> allSockets = webSockets.keySet();
            allSockets.stream()
                .filter(socket -> {
                    // verify that the socket was not just closed in the meantime...
                    try {
                        socket.broadcast(allSockets, messageAsString);
                        return true;
                    } catch (Exception ex) {
                        // arrg, looks like the socket was just closed. So we fail silently and hope for the next one
                    }
                    return false;
                })
                .findAny();
                // System.out.println("Successfully broadcast? " + broadcastSocket.isPresent());
        };

        Consumer<CloseReason> closeAction = closeReason -> {
            subscriptionConnections.dispose();
            subscriptionDisconnections.dispose();
            Set<org.glassfish.grizzly.websockets.WebSocket> allSockets = webSockets.keySet();
            allSockets.forEach(s -> s.close(closeReason.code, closeReason.text));
            webSockets.clear();
            app.close();
        };
        return new ServerEndpoint<>(endpointStreamSource, broadcastAction, closeAction);
    }
}
