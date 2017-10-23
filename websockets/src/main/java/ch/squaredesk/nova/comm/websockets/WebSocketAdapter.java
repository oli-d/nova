package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.websockets.WebsocketAdapter.client.ClientEndpoint;
import ch.squaredesk.nova.comm.websockets.client.StreamCreatingWebSocketTextListener;
import ch.squaredesk.nova.comm.websockets.WebsocketAdapter.server.ServerEndpoint;
import ch.squaredesk.nova.comm.websockets.server.StreamCreatingWebSocketApplication;
import ch.squaredesk.nova.comm.websockets.client.ClientEndpoint;
import ch.squaredesk.nova.comm.websockets.server.ServerEndpoint;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.tuples.Pair;
import ch.squaredesk.nova.tuples.Tuple3;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ws.WebSocketUpgradeHandler;
import io.reactivex.BackpressureStrategy;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class WebSocketAdapter<MessageType> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketAdapter.class);

    private final HttpServer httpServer;
    private final AsyncHttpClient httpClient;

    private final CopyOnWriteArraySet<String> registeredDestinations = new CopyOnWriteArraySet<>();
    private final MessageMarshaller<MessageType, String> messageMarshaller;
    private final MessageUnmarshaller<String, MessageType> messageUnmarshaller;
    private final MetricsCollector metricsCollector;

    public WebSocketAdapter(Builder<MessageType> builder) {
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
        return string -> webSocket.sendMessage(string);
    }

    private Runnable rawCloseActionFor(com.ning.http.client.ws.WebSocket webSocket) {
        return () -> webSocket.close();
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
        return string -> webSocket.send(string);
    }

    private Runnable rawCloseActionFor(org.glassfish.grizzly.websockets.WebSocket webSocket) {
        return () -> webSocket.close();
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
            metricsCollector.unparsableMessageReceived(destination);
            throw new RuntimeException("Unable to unmarshal incoming message " + message + " on destination " + destination, e);
        }
    }

    public ClientEndpoint<MessageType> connectTo (String destination)  {
        if (httpClient==null) {
            throw new IllegalStateException("Adapter not initialized properly for client mode");
        }

        StreamCreatingWebSocketTextListener listener =
                new StreamCreatingWebSocketTextListener<>(text -> unmarshal(destination, text));
        WebSocketUpgradeHandler webSocketUpgradeHandler =new WebSocketUpgradeHandler.Builder()
                .addWebSocketListener(listener)
                .build();
        com.ning.http.client.ws.WebSocket underlyingWebSocket = openConnection(destination, webSocketUpgradeHandler);

        // FIXME: when closed, clean up
        WebSocket<MessageType> webSocket = createWebSocket(destination, underlyingWebSocket);

        BackpressureStrategy backpressureStrategy = BackpressureStrategy.BUFFER; // FIXME
        return new ClientEndpoint<>(
                messageSubject.toFlowable(backpressureStrategy).map(message -> new Tuple3<>(message, destination, underlyingWebSocket)),
                connectionSubject.toFlowable(backpressureStrategy).map(dummy -> underlyingWebSocket),
                closeSubject.toFlowable(backpressureStrategy).map(dummy -> new Pair<>(underlyingWebSocket, null)),
                errorSubject.toFlowable(backpressureStrategy).map(t -> new Pair<>(underlyingWebSocket, t)),
                webSocket);
    }

    public ServerEndpoint<MessageType> acceptConnections(String destination)  {
        if (httpServer == null) {
            throw new IllegalStateException("Adapter not initialized properly for server mode");
        }

        String destinationToUse = destination.startsWith("/") ? destination : "/" + destination;

        // TODO: do we need toSerialized versions? grizzly is nio, though...
        StreamCreatingWebSocketApplication app =
                new StreamCreatingWebSocketApplication<>(text -> unmarshal(destinationToUse, text));
        /*
        {
            @Override
            public void onClose(org.glassfish.grizzly.websockets.WebSocket socket, DataFrame frame) {
                // FIXME: convert dataFrame to something useful
                closeSubject.onNext(new Pair<>(socket, frame));
                allSockets.remove(socket);
                metricsCollector.subscriptionDestroyed(destinationToUse);
            }

            @Override
            public void onConnect(org.glassfish.grizzly.websockets.WebSocket socket) {
                connectionSubject.onNext(socket);
                allSockets.add(socket);
                metricsCollector.subscriptionCreated(destinationToUse);
            }

            @Override
            protected boolean onError(org.glassfish.grizzly.websockets.WebSocket socket, Throwable t) {
                errorSubject.onNext(new Pair<>(socket, t));
                // TODO metrics?
                // TODO verify: is onClose() invoked?
                return true; // close webSocket
            }

            @Override
            public void onMessage(org.glassfish.grizzly.websockets.WebSocket socket, String text) {
                try {
                    MessageType message = messageUnmarshaller.unmarshal(text);
                    messageSubject.onNext(new Tuple3<>(message, destinationToUse, socket));
                    metricsCollector.messageReceived(destinationToUse);
                } catch (Exception e) {
                    logger.error("Unable to unmarshal incoming message " + text + " on destination " + destinationToUse, e);
                    metricsCollector.unparsableMessageReceived(destinationToUse);
                }
            }
        };
        */
        WebSocketEngine.getEngine().register("", destinationToUse, app);

        BackpressureStrategy backpressureStrategy = BackpressureStrategy.BUFFER; // FIXME
        // FIXME: when closed, clean up
        Consumer<MessageType> broadcastAction = message -> {
            String messageAsString;
            try {
                messageAsString = messageMarshaller.marshal(message);
            } catch (Exception e) {
                // TODO: metric?
                throw new RuntimeException("Unable to marshal message " + message, e);
            }
            socket.broadcast(allSockets, messageAsString);
        };
        return new ServerEndpoint<>(
                messageSubject.toFlowable(backpressureStrategy),
                connectionSubject.toFlowable(backpressureStrategy),
                closeSubject.toFlowable(backpressureStrategy),
                errorSubject.toFlowable(backpressureStrategy),
                socket -> createWebSocket(destinationToUse, socket),
                broadcastAction);
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
            this.httpServer = httpServer;
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
