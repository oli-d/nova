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
import ch.squaredesk.nova.comm.websockets.annotation.client.ClientEndpointFactory;
import ch.squaredesk.nova.comm.websockets.annotation.server.ServerEndpoint;
import ch.squaredesk.nova.comm.websockets.annotation.server.ServerEndpointFactory;
import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketAddOn;

import java.util.concurrent.CopyOnWriteArraySet;

import static java.util.Objects.requireNonNull;

public class WebSocketAdapter<MessageType> {
    private final HttpServer httpServer;
    private final AsyncHttpClient httpClient;

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



    public ClientEndpoint<MessageType> connectTo (String destination)  {
        if (httpClient==null) {
            throw new IllegalStateException("Adapter not initialized properly for client mode");
        }

        return ClientEndpointFactory.createFor(
                httpClient,
                destination,
                messageMarshaller,
                messageUnmarshaller,
                metricsCollector);
    }

    public ServerEndpoint<MessageType> acceptConnections(String destination)  {
        if (httpServer == null) {
            throw new IllegalStateException("Adapter not initialized properly for server mode");
        }

        return ServerEndpointFactory.createFor(
                destination,
                messageMarshaller,
                messageUnmarshaller,
                metricsCollector
        );
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
