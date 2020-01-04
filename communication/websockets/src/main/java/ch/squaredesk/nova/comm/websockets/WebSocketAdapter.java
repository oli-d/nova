/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */
package ch.squaredesk.nova.comm.websockets;

import ch.squaredesk.nova.comm.CommAdapter;
import ch.squaredesk.nova.comm.CommAdapterBuilder;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.http.HttpServerInstanceListener;
import ch.squaredesk.nova.comm.websockets.client.WebSocketFactory;
import ch.squaredesk.nova.comm.websockets.server.ServerEndpointFactory;
import com.ning.http.client.AsyncHttpClient;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class WebSocketAdapter extends CommAdapter<String> implements HttpServerInstanceListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketAdapter.class);

    private HttpServer httpServer;
    private final AsyncHttpClient httpClient;
    private final MetricsCollector metricsCollector;
    private final ServerEndpointFactory serverEndpointFactory;
    private final WebSocketFactory clientEndpointFactory;
    private final ConcurrentHashMap<String, BehaviorSubject<WebSocket>> connectingWebSocketsByDestination = new ConcurrentHashMap<>();

    private WebSocketAdapter(Builder builder) {
        super(builder.messageTranscriber, builder.metrics);
        this.metricsCollector = new MetricsCollector(builder.identifier, builder.metrics);
        setHttpServer(builder.httpServer);
        this.httpClient = builder.httpClient;
        this.serverEndpointFactory = new ServerEndpointFactory();
        this.clientEndpointFactory = new WebSocketFactory(builder.messageTranscriber);
    }

    private void setHttpServer(HttpServer httpServer) {
        if (httpServer !=null) {
            this.httpServer = httpServer;

//            if (httpServer.isStarted()) {
//                throw new IllegalArgumentException("HttpServer MUST NOT BE STARTED before WebSocketAdapter is created");
//            }
            // TODO: would be cool, if we could somehow find out whether this was already done
            WebSocketAddOn addon = new WebSocketAddOn();
            for (NetworkListener listener : httpServer.getListeners()) {
                listener.registerAddOn(addon);
            }
        }
    }

    public WebSocket connectTo (String destination)  {
        if (httpClient==null) {
            throw new IllegalStateException("Adapter not initialized properly for client mode");
        }
        return clientEndpointFactory.createFor(httpClient, destination, metricsCollector);
    }

    public Observable<WebSocket> acceptConnections(String destination)  {
        if (httpServer == null) {
            throw new IllegalStateException("Adapter not initialized properly for server mode");
        }

        String destinationForSubscription = destination.startsWith("/") ? destination : "/" + destination;

        return connectingWebSocketsByDestination.computeIfAbsent(destinationForSubscription,
                dest -> {
                    BehaviorSubject<WebSocket> subject = BehaviorSubject.create();

                    WebSocketApplication app = new WebSocketApplication() {
                        @Override
                        public void onConnect(org.glassfish.grizzly.websockets.WebSocket socket) {
                            WebSocket serverSideWebSocket = serverEndpointFactory.createFor(dest, socket, messageTranscriber, metricsCollector);
                            subject.onNext(serverSideWebSocket);
                        }

//                        @Override
//                        public void onClose(org.glassfish.grizzly.websockets.WebSocket socket, DataFrame frame) {
//                        }
//
            //  FIXME          @Override
            //            protected boolean onError(org.glassfish.grizzly.websockets.WebSocket socket, Throwable t) {
            //                errors.onNext(new Pair<>(socket, t));
            //                return true; // close webSocket
            //            }
                    };
                    WebSocketEngine.getEngine().register("", dest, app);
                    return subject;
                });

    }

    @Override
    public void httpServerInstanceCreated(HttpServer httpServer) {
        if (this.httpServer == null) {
            logger.info("httpServer available, server functionality available");
            setHttpServer(httpServer);
        }
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends CommAdapterBuilder<String, WebSocketAdapter>{
        public String identifier;
        private HttpServer httpServer;
        private AsyncHttpClient httpClient;

        private Builder() {
        }

        public Builder setIdentifier (String identifier) {
            this.identifier = identifier;
            return this;
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
            if (httpClient == null) {
                httpClient = new AsyncHttpClient();
            }
            if (messageTranscriber == null) {
                messageTranscriber = new DefaultMessageTranscriberForStringAsTransportType();
            }
            return new WebSocketAdapter(this);
        }
    }
}
