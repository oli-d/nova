/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets.annotation;

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.websockets.annotation.client.ClientEndpoint;
import ch.squaredesk.nova.comm.websockets.annotation.client.StreamCreatingWebSocketTextListener;
import ch.squaredesk.nova.comm.websockets.annotation.server.ServerEndpoint;
import ch.squaredesk.nova.comm.websockets.annotation.server.StreamCreatingWebSocketApplication;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.tuples.Pair;
import ch.squaredesk.nova.tuples.Tuple3;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ws.WebSocketUpgradeHandler;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class WebSocketAdapter<MessageType> {
    private final HttpServer httpServer;
    private final AsyncHttpClient httpClient;

    private final CopyOnWriteArraySet<String> registeredDestinations = new CopyOnWriteArraySet<>();
    private final MessageMarshaller<MessageType, String> messageMarshaller;
    private final MessageUnmarshaller<String, MessageType> messageUnmarshaller;
    private final MetricsCollector metricsCollector;

    private WebSocketAdapter(Builder<MessageType> builder) {
        this.messageMarshaller = builder.messageMarshaller;
        this.messageUnmarshaller = builder.messageUnmarshaller;
        this.metricsCollector = new MetricsCollector(builder.metrics);
        this.httpServer = builder.httpServer;
        if (httpServer !=null) {
            WebSocketAddOn addon = new WebSocketAddOn();
            for (NetworkListener listener : httpServer.getListeners()) {
                listener.registerAddOn(addon);
            }
        }
        this.httpClient = builder.httpClient;
    }


    ////////////////
    //
    // Client specific
    //
    ////////////////
    private com.ning.http.client.ws.WebSocket openConnection(String destination,
                                                             WebSocketUpgradeHandler webSocketUpgradeHandler) {
        try {
            return httpClient.prepareGet(destination).execute(webSocketUpgradeHandler).get();
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to " + destination, e);
        }
    }

    private Consumer<String> rawSendActionFor(com.ning.http.client.ws.WebSocket webSocket) {
        return webSocket::sendMessage;
    }

    private Runnable rawCloseActionFor(com.ning.http.client.ws.WebSocket webSocket) {
        return webSocket::close;
    }


    private WebSocket<MessageType> createWebSocket(String destination, com.ning.http.client.ws.WebSocket webSocket) {
        Consumer<MessageType> sendAction = sendActionFor(destination, rawSendActionFor(webSocket));
        Runnable closeAction = closeActionFor(destination, rawCloseActionFor(webSocket));
        return new WebSocket<>(sendAction, closeAction);
    }


    ////////////////
    //
    // Server specific
    //
    ////////////////
    private Consumer<String> rawSendActionFor(org.glassfish.grizzly.websockets.WebSocket webSocket) {
        return webSocket::send;
    }

    private Runnable rawCloseActionFor(org.glassfish.grizzly.websockets.WebSocket webSocket) {
        return webSocket::close;
    }

    private WebSocket<MessageType> createWebSocket(String destination, org.glassfish.grizzly.websockets.WebSocket webSocket) {
        Consumer<MessageType> sendAction = sendActionFor(destination, rawSendActionFor(webSocket));
        Runnable closeAction = closeActionFor(destination, rawCloseActionFor(webSocket));
        return new WebSocket<>(sendAction, closeAction);
    }


    ////////////////
    //
    // Generic
    //
    ////////////////
    private static String destinationForMetrics (String destination) {
        return destination.startsWith("/") ? destination.substring(1) : destination;
    }

    private static <SomeMessageType, SomeWebSocketType> EndpointStreamSource<SomeMessageType> createStreamSourceFor(
            String destination,
            Function<SomeWebSocketType, WebSocket<SomeMessageType>> webSocketFactory,
            StreamCreatingEndpointWrapper<SomeWebSocketType, SomeMessageType> streamCreatingEndpointWrapper,
            MetricsCollector metricsCollector) {

        String destinationForMetrics = destinationForMetrics(destination);

        Observable<Tuple3<SomeMessageType, String, WebSocket<SomeMessageType>>> messages = streamCreatingEndpointWrapper.messages()
                .map(pair -> new Tuple3<>(pair._2, destination, webSocketFactory.apply(pair._1)))
                .doOnNext(tuple -> metricsCollector.messageReceived(destinationForMetrics));
        Observable<WebSocket<SomeMessageType>> connectingSockets = streamCreatingEndpointWrapper.connectingSockets()
                .map(webSocketFactory::apply)
                .doOnNext(socket -> metricsCollector.subscriptionCreated(destinationForMetrics));
        Observable<Pair<WebSocket<SomeMessageType>, CloseReason>> closingSockets = streamCreatingEndpointWrapper.closingSockets()
                .map(pair -> new Pair<>(webSocketFactory.apply(pair._1), pair._2))
                .doOnNext(socket -> metricsCollector.subscriptionDestroyed(destinationForMetrics));
        Observable<Pair<WebSocket<SomeMessageType>, Throwable>> errors = streamCreatingEndpointWrapper.errors()
                .map(pair -> new Pair<>(webSocketFactory.apply(pair._1), pair._2)); // TODO metric?
        return new EndpointStreamSource<>(
                messages,
                connectingSockets,
                closingSockets,
                errors);
    }

    private Consumer<MessageType> sendActionFor(String destination, Consumer<String> rawSendAction) {
        return message -> {
            String messageAsString = marshal(message);
            rawSendAction.accept(messageAsString);
            metricsCollector.messageSent(destination);
        };
    }

    private Runnable closeActionFor (String destination, Runnable rawCloseAction) {
        return () -> {
            rawCloseAction.run();
            metricsCollector.subscriptionDestroyed(destination);
        };
    }

    private String marshal (MessageType message) {
        try {
            return messageMarshaller.marshal(message);
        } catch (Exception e) {
            // TODO: metric?
            throw new RuntimeException("Unable to marshal message " + message, e);
        }
    }

    private MessageType unmarshal (String destination, String message) {
        try {
            return messageUnmarshaller.unmarshal(message);
        } catch (Exception e) {
            metricsCollector.unparsableMessageReceived(destinationForMetrics(destination));
            throw new RuntimeException("Unable to unmarshal incoming message " + message + " on destination " + destination, e);
        }
    }

    public ClientEndpoint<MessageType> connectTo (String destination)  {
        if (httpClient==null) {
            throw new IllegalStateException("Adapter not initialized properly for client mode");
        }

        StreamCreatingWebSocketTextListener<MessageType> listener =
                new StreamCreatingWebSocketTextListener<>(text -> unmarshal(destination, text));
        WebSocketUpgradeHandler webSocketUpgradeHandler =new WebSocketUpgradeHandler.Builder()
                .addWebSocketListener(listener)
                .build();
        com.ning.http.client.ws.WebSocket underlyingWebSocket = openConnection(destination, webSocketUpgradeHandler);

        // FIXME: everything released when closed???
        WebSocket<MessageType> webSocket = createWebSocket(destination, underlyingWebSocket);
        Function<com.ning.http.client.ws.WebSocket, WebSocket<MessageType>> webSocketFactory = rawSocket -> webSocket;

        EndpointStreamSource<MessageType> endpointStreamSource = createStreamSourceFor(destination, webSocketFactory, listener, metricsCollector);

        Consumer<CloseReason> closeAction = closeReason -> {
            underlyingWebSocket.close();
            listener.close();
        };
        return new ClientEndpoint<>(endpointStreamSource, webSocket, closeAction);
    }

    public ServerEndpoint<MessageType> acceptConnections(String destination)  {
        if (httpServer == null) {
            throw new IllegalStateException("Adapter not initialized properly for server mode");
        }

        String destinationToUse = destination.startsWith("/") ? destination : "/" + destination;

        StreamCreatingWebSocketApplication<MessageType> app =
                new StreamCreatingWebSocketApplication<>(text -> unmarshal(destinationToUse, text));
        WebSocketEngine.getEngine().register("", destinationToUse, app);

        // FIXME: everything released when closed???
        Function<org.glassfish.grizzly.websockets.WebSocket, WebSocket<MessageType>> webSocketFactory
                = socket -> createWebSocket(destinationToUse, socket);
        EndpointStreamSource<MessageType> endpointStreamSource =
                createStreamSourceFor(destination, webSocketFactory, app, metricsCollector);

        Set<org.glassfish.grizzly.websockets.WebSocket> allSockets = new CopyOnWriteArraySet<>(); // FIXME: proper data structure
        Disposable subscriptionConnections = app.connectingSockets().subscribe(allSockets::add);
        Disposable subscriptionDisconnections = app.closingSockets().subscribe(pair -> allSockets.remove(pair._1));
        Consumer<MessageType> broadcastAction = message -> {
            String messageAsString;
            try {
                messageAsString = messageMarshaller.marshal(message);
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


    public static <MessageType> Builder<MessageType> builder() {
        return new Builder<>();
    }

    public static class Builder<MessageType> {
        private MessageMarshaller<MessageType, String> messageMarshaller;
        private MessageUnmarshaller<String, MessageType> messageUnmarshaller;
        private Metrics metrics;
        private HttpServer httpServer;
        private AsyncHttpClient httpClient;

        private Builder() {
        }

        public Builder<MessageType> setHttpClient (AsyncHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder<MessageType> setHttpServer (HttpServer httpServer) {
            this.httpServer = httpServer;
            return this;
        }

        public Builder<MessageType> setMetrics (Metrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder<MessageType> setMessageMarshaller(MessageMarshaller<MessageType, String> marshaller) {
            this.messageMarshaller = marshaller;
            return this;
        }

        public Builder<MessageType> setMessageUnmarshaller(MessageUnmarshaller<String, MessageType> unmarshaller) {
            this.messageUnmarshaller = unmarshaller;
            return this;
        }

        private void validate() {
            requireNonNull(metrics, "metrics must be provided");
            requireNonNull(messageMarshaller, " messageMarshaller instance must not be null");
            requireNonNull(messageUnmarshaller, " messageUnmarshaller instance must not be null");
        }

        public WebSocketAdapter<MessageType> build() {
            validate();
            return new WebSocketAdapter<>(this);
        }
    }
}
