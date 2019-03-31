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

import ch.squaredesk.nova.comm.CommAdapter;
import ch.squaredesk.nova.comm.CommAdapterBuilder;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.websockets.client.ClientEndpoint;
import ch.squaredesk.nova.comm.websockets.client.ClientEndpointFactory;
import ch.squaredesk.nova.comm.websockets.server.ServerEndpoint;
import ch.squaredesk.nova.comm.websockets.server.ServerEndpointFactory;
import com.ning.http.client.AsyncHttpClient;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketAddOn;

public class WebSocketAdapter extends CommAdapter<String> {
    private final HttpServer httpServer;
    private final AsyncHttpClient httpClient;

    private final MetricsCollector metricsCollector;
    private final ServerEndpointFactory serverEndpointFactory;
    private final ClientEndpointFactory clientEndpointFactory;

    private WebSocketAdapter(Builder builder) {
        super(builder.messageTranscriber, builder.metrics);
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
        this.serverEndpointFactory = new ServerEndpointFactory(builder.messageTranscriber);
        this.clientEndpointFactory = new ClientEndpointFactory(builder.messageTranscriber);
    }

    public ClientEndpoint connectTo (String destination)  {
        if (httpClient==null) {
            throw new IllegalStateException("Adapter not initialized properly for client mode");
        }
        return clientEndpointFactory.createFor(httpClient, destination, metricsCollector);
    }

    public ServerEndpoint acceptConnections(String destination)  {
        if (httpServer == null) {
            throw new IllegalStateException("Adapter not initialized properly for server mode");
        }
        return serverEndpointFactory.createFor(destination, metricsCollector);
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends CommAdapterBuilder<String, WebSocketAdapter>{
        private HttpServer httpServer;
        private AsyncHttpClient httpClient;

        private Builder() {
        }

        public Builder setHttpClient (AsyncHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder setHttpServer (HttpServer httpServer) {
            this.httpServer = httpServer;
            return this;
        }

        public WebSocketAdapter createInstance() {
            validate();
            if (messageTranscriber == null) {
                messageTranscriber = new DefaultMessageTranscriberForStringAsTransportType();
            }
            return new WebSocketAdapter(this);
        }
    }
}
