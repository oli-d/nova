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
import io.reactivex.disposables.Disposable;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServerEndpointFactory {
    private static <MessageType> WebSocket<MessageType> createWebSocket(
            String destination,
            org.glassfish.grizzly.websockets.WebSocket webSocket,
            MessageMarshaller<MessageType, String> messageMarshaller,
            MetricsCollector metricsCollector) {

        return new WebSocket<>(
                message -> {
                    String messageAsString = marshal(message, messageMarshaller);
                    webSocket.send(messageAsString);
                    if (metricsCollector != null) { // we could optimize and remove the if, but for now we rely on JIT compilation
                        metricsCollector.messageSent(destination);
                    }
                },
                () -> {
                    webSocket.close();
                    if (metricsCollector != null) { // we could optimize and remove the if, but for now we rely on JIT compilation
                        metricsCollector.subscriptionDestroyed(destination);
                    }
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
            if (metricsCollector != null) {
                metricsCollector.unparsableMessageReceived(destination);
            }
            throw new RuntimeException("Unable to unmarshal incoming message " + message + " on destination " + destination, e);
        }
    }

    public static <MessageType> ServerEndpoint<MessageType> createFor(
            String destination,
            MessageMarshaller<MessageType, String> messageMarshaller,
            MessageUnmarshaller<String, MessageType> messageUnmarshaller,
            MetricsCollector metricsCollector)  {

        String destinationForSubscription = destination.startsWith("/") ? destination : "/" + destination;
        String destinationForMetrics = destination.startsWith("/") ? destination.substring(1) : destination;

        StreamCreatingWebSocketApplication<MessageType> app =
                new StreamCreatingWebSocketApplication<>(text -> unmarshal(destinationForMetrics, text, messageUnmarshaller, metricsCollector));
        WebSocketEngine.getEngine().register("", destinationForSubscription, app);

        // FIXME: everything released when closed???
        Function<org.glassfish.grizzly.websockets.WebSocket, WebSocket<MessageType>> webSocketFactory =
                socket -> createWebSocket(destinationForMetrics, socket, messageMarshaller, metricsCollector);

        EndpointStreamSource<MessageType> endpointStreamSource =
                EndpointStreamSourceFactory.createStreamSourceFor(destinationForMetrics, webSocketFactory, app, metricsCollector);

        Set<org.glassfish.grizzly.websockets.WebSocket> allSockets = new CopyOnWriteArraySet<>(); // FIXME: proper data structure
        Disposable subscriptionConnections = app.connectingSockets().subscribe(allSockets::add);
        Disposable subscriptionDisconnections = app.closingSockets().subscribe(pair -> allSockets.remove(pair._1));
        Consumer<MessageType> broadcastAction = message -> {
            String messageAsString;
            try {
                messageAsString = marshal(message, messageMarshaller);
            } catch (Exception e) {
                // TODO: metric?
                throw new RuntimeException("Unable to marshal broadcast message " + message, e);
            }

            // Optional<org.glassfish.grizzly.websockets.WebSocket> broadcastSocket =
            allSockets
            .stream()
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
            allSockets.forEach(s -> s.close(closeReason.code, closeReason.text));
            allSockets.clear();
            app.close();
        };
        return new ServerEndpoint<>(endpointStreamSource, broadcastAction, closeAction);
    }
}
