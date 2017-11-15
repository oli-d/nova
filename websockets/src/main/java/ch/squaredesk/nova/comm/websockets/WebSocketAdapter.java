/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.CommAdapterBuilder;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.websockets.client.ClientEndpoint;
import ch.squaredesk.nova.comm.websockets.client.ClientEndpointFactory;
import ch.squaredesk.nova.comm.websockets.server.ServerEndpoint;
import ch.squaredesk.nova.comm.websockets.server.ServerEndpointFactory;
import com.ning.http.client.AsyncHttpClient;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketAddOn;

public class WebSocketAdapter<MessageType> {
    private final HttpServer httpServer;
    private final AsyncHttpClient httpClient;

    private final MessageMarshaller<MessageType, String> messageMarshaller;
    private final MessageUnmarshaller<String, MessageType> messageUnmarshaller;
    private final MetricsCollector metricsCollector;
    private final ServerEndpointFactory serverEndpointFactory = new ServerEndpointFactory();

    private WebSocketAdapter(Builder<MessageType> builder) {
        this.messageMarshaller = builder.messageMarshaller;
        this.messageUnmarshaller = builder.messageUnmarshaller;
        this.metricsCollector = new MetricsCollector(builder.metrics);
        this.httpServer = builder.httpServer;
        if (httpServer !=null) {
//            if (httpServer.isStarted()) {
//                throw new IllegalArgumentException("HttpServer MUST NOT BE STARTED before WebSocketAdapter is created");
//            }
            // TODO: would be cool, if we could somehow find out whether this was already done
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

        return serverEndpointFactory.createFor(
                destination,
                messageMarshaller,
                messageUnmarshaller,
                metricsCollector
        );
    }


    public static <MessageType> Builder<MessageType> builder(Class<MessageType> messageTypeClass) {
        return new Builder<>(messageTypeClass);
    }

    public static class Builder<MessageType> extends CommAdapterBuilder<MessageType, WebSocketAdapter<MessageType>>{
        private HttpServer httpServer;
        private AsyncHttpClient httpClient;

        private Builder(Class<MessageType> messageTypeClass) {
            super(messageTypeClass);
        }

        public Builder<MessageType> setHttpClient (AsyncHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder<MessageType> setHttpServer (HttpServer httpServer) {
            this.httpServer = httpServer;
            return this;
        }

        public WebSocketAdapter<MessageType> createInstance() {
            validate();
            return new WebSocketAdapter<>(this);
        }
    }
}
