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

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.websockets.*;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.grizzly.websockets.WebSocketListener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ServerEndpointFactory {
    private static final Scheduler lifecycleEventScheduler = Schedulers.io();
    private final ConcurrentHashMap<org.glassfish.grizzly.websockets.WebSocket, WebSocket> webSockets = new ConcurrentHashMap<>();
    private final MessageTranscriber<String> messageTranscriber;

    public ServerEndpointFactory(MessageTranscriber<String> messageTranscriber) {
        this.messageTranscriber = messageTranscriber;
    }

    private WebSocket instantiateNewWebSocket(org.glassfish.grizzly.websockets.WebSocket webSocket) {
        SendAction sendAction = new SendAction() {
            @Override
            public <T> void accept(T message) throws Exception {
                String messageAsString = messageTranscriber.getOutgoingMessageTranscriber(message).apply(message);
                webSocket.send(messageAsString);
            }
        };
        Single<Long> closeEventSingle = Single.create(s -> {
            webSocket.add(new WebSocketListener() {
                @Override
                public void onClose(org.glassfish.grizzly.websockets.WebSocket socket, DataFrame frame) {
                    s.onSuccess(System.currentTimeMillis());
                }

                @Override
                public void onConnect(org.glassfish.grizzly.websockets.WebSocket socket) {
                }

                @Override
                public void onMessage(org.glassfish.grizzly.websockets.WebSocket socket, String text) {
                }

                @Override
                public void onMessage(org.glassfish.grizzly.websockets.WebSocket socket, byte[] bytes) {
                }

                @Override
                public void onPing(org.glassfish.grizzly.websockets.WebSocket socket, byte[] bytes) {
                }

                @Override
                public void onPong(org.glassfish.grizzly.websockets.WebSocket socket, byte[] bytes) {
                }

                @Override
                public void onFragment(org.glassfish.grizzly.websockets.WebSocket socket, String fragment, boolean last) {
                }

                @Override
                public void onFragment(org.glassfish.grizzly.websockets.WebSocket socket, byte[] fragment, boolean last) {
                }
            });
        });

        return new WebSocket(sendAction, webSocket::close, webSocket::isConnected, closeEventSingle);
    }

    private WebSocket createWebSocket(org.glassfish.grizzly.websockets.WebSocket webSocket) {
        return webSockets.computeIfAbsent(webSocket, this::instantiateNewWebSocket);
    }

    public WebSocket createFor(
            String destination,
            MetricsCollector metricsCollector)  {

        String destinationForSubscription = destination.startsWith("/") ? destination : "/" + destination;
        String destinationForMetrics = destination.startsWith("/") ? destination.substring(1) : destination;

        StreamCreatingWebSocketApplication app = new StreamCreatingWebSocketApplication();
        WebSocketEngine.getEngine().register("", destinationForSubscription, app);

        Function<org.glassfish.grizzly.websockets.WebSocket, WebSocket> webSocketCreator = this::createWebSocket;

        EndpointStreamSource endpointStreamSource =
                EndpointStreamSourceFactory.createStreamSourceFor(destinationForMetrics, webSocketCreator, app, metricsCollector);

        // register all connecting WebSockets
        Disposable subscriptionConnections = app.connectingSockets()
                .subscribeOn(lifecycleEventScheduler).subscribe(webSocketCreator::apply);
        // unregister all disconnecting WebSockets
        Disposable subscriptionDisconnections = app.closingSockets()
                .subscribeOn(lifecycleEventScheduler).subscribe(pair -> webSockets.remove(pair._1));
        SendAction broadcastAction = new SendAction() {
            @Override
            public <T> void accept(T message) throws Exception {
                String messageAsString = messageTranscriber.getOutgoingMessageTranscriber(message).apply(message);

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
            }
        };

        Consumer<CloseReason> closeAction = closeReason -> {
            subscriptionConnections.dispose();
            subscriptionDisconnections.dispose();
            Set<org.glassfish.grizzly.websockets.WebSocket> allSockets = webSockets.keySet();
            allSockets.forEach(s -> s.close(closeReason.code, closeReason.text));
            webSockets.clear();
            app.close();
        };
        return new ServerEndpoint(destination, endpointStreamSource, broadcastAction, closeAction, messageTranscriber, metricsCollector);
    }
}
