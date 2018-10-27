/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.CommAdapter;
import ch.squaredesk.nova.comm.CommAdapterBuilder;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import com.ning.http.client.AsyncHttpClient;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class HttpAdapter extends CommAdapter<String> {
    private final RpcClient rpcClient;
    private final RpcServer rpcServer;
    private final Long defaultRequestTimeout;
    private final TimeUnit defaultRequestTimeUnit;


    private HttpAdapter(Builder builder) {
        super(builder.messageTranscriber, builder.metrics);
        this.rpcClient = builder.rpcClient;
        this.rpcServer = builder.rpcServer;
        this.defaultRequestTimeout = builder.defaultRequestTimeout;
        this.defaultRequestTimeUnit = builder.defaultRequestTimeUnit;
    }

    public <T> FrozenHttpAdapter<T> freeze (Class<T> typeClass) {
        return new FrozenHttpAdapter<>(this, typeClass);
    }

    ///////// The client side
    /////////
    /////////
    /////////////////// GET convenience methods
    public <U> Single<RpcReply<U>> sendGetRequest(String destination, Class<U> replyType) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET), replyType, null, null );
    }

    public <U> Single<RpcReply<U>> sendGetRequest(
                String destination,
                Class<U> replyType,
                long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET), replyType, timeout, timeUnit);
    }

    /////////////////// POST convenience methods
    public <T, U> Single<RpcReply<U>> sendPostRequest(
                String destination,
                T request,
                Class<U> replyType) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST), replyType, null, null);
    }

    public <T, U> Single<RpcReply<U>> sendPostRequest(
                String destination,
                T request,
                Class<U> replyType,
                long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST), replyType, timeout, timeUnit );
    }

    /////////////////// PUT convenience methods
    public <T, U> Single<RpcReply<U>> sendPutRequest(
                String destination,
                T request,
                Class<U> replyType) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT), replyType, null, null);
    }

    public <T, U> Single<RpcReply<U>> sendPutRequest(
                String destination,
                T request,
                Class<U> replyType,
                long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT), replyType, timeout, timeUnit );
    }

    /////////////////// DELETE convenience methods
    public <T, U> Single<RpcReply<U>> sendDeleteRequest(
                String destination,
                T request,
                Class<U> replyType) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.DELETE), replyType, null, null);
    }

    public <T, U> Single<RpcReply<U>> sendDeleteRequest(
                String destination,
                T request,
                Class<U> replyType,
                long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.DELETE), replyType, timeout, timeUnit );
    }

    /////////////////// convenience methods
    public <T, U> Single<RpcReply<U>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Class<U>replyType) {
        return sendRequest(destination, request, new RequestInfo(requestMethod), replyType, null, null);
    }


    public <T, U> Single<RpcReply<U>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Class<U> replyType,
                long timeout,
                TimeUnit timeUnit) {
        return sendRequest(destination, request, new RequestInfo(requestMethod), replyType, timeout, timeUnit);
    }

    /////////////////// finally, the implementation
    public <T, U> Single<RpcReply<U>> sendRequest (
                String destination,
                T request,
                RequestInfo httpInfo,
                Class<U> replyType,
                Long timeout, TimeUnit timeUnit) {

        if (timeout!=null) {
            requireNonNull(timeUnit, "timeUnit must not be null if timeout specified");
        } else {
            timeout = defaultRequestTimeout;
            timeUnit = defaultRequestTimeUnit;
        }

        URL url;
        try {
            url = new URL(destination);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format " + destination,e);
        }

        RequestMessageMetaData sendingInfo = new RequestMessageMetaData(url, httpInfo);

        return rpcClient.sendRequest(request, sendingInfo, replyType, messageTranscriber, timeout, timeUnit);
    }

    ///////// The server side
    /////////
    /////////
    public <T> Flowable<RpcInvocation<T>> requests(String destination, Class<T> requestType) {
        return rpcServer.requests(destination, messageTranscriber, requestType);
    }

    public void start() throws Exception {
        rpcServer.start();
    }

    public void shutdown() {
        if (rpcServer!=null) {
            rpcServer.shutdown();
        }
        rpcClient.shutdown();
    }

    ///////// The builder
    /////////
    /////////
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends CommAdapterBuilder<String, HttpAdapter>{
        private static Logger logger = LoggerFactory.getLogger(Builder.class);

        private String identifier;
        private HttpServer httpServer;
        private RpcClient rpcClient;
        private RpcServer rpcServer;
        private Long defaultRequestTimeout;
        private TimeUnit defaultRequestTimeUnit;

        private Builder() {
        }

        public Builder setDefaultRequestTimeout(long timeout, TimeUnit timeUnit) {
            requireNonNull(timeUnit);
            if (timeout>0) {
                defaultRequestTimeout = timeout;
                defaultRequestTimeUnit = timeUnit;
            }
            return this;
        }

        public Builder setHttpServer(HttpServer httpServer) {
            this.httpServer = httpServer;
            return this;
        }

        public Builder setMessageTranscriber(MessageTranscriber<String> val) {
            super.setMessageTranscriber(val);
            return this;
        }

        public Builder setRpcServer(RpcServer rpcServer) {
            this.rpcServer = rpcServer;
            return this;
        }

        public Builder setRpcClient(RpcClient rpcClient) {
            this.rpcClient = rpcClient;
            return this;
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        protected void validate() {
        }

        protected HttpAdapter createInstance() {
            validate();
            if (messageTranscriber == null) {
                messageTranscriber = new DefaultMessageTranscriberForStringAsTransportType();
            }
            if (defaultRequestTimeout==null) {
                defaultRequestTimeout = 15L;
                defaultRequestTimeUnit = TimeUnit.SECONDS;
            }
            if (rpcClient == null) {
                AsyncHttpClient httpClient = new AsyncHttpClient();
                rpcClient = new RpcClient(identifier, httpClient, metrics);
            }
            if (rpcServer == null) {
                if (httpServer == null) {
                    logger.info("No httpServer provided, HTTP Adapter will only be usable in client mode!!!");
                } else {
                    rpcServer = new RpcServer(identifier, httpServer, metrics);
                }
            }
            return new HttpAdapter(this);
        }
    }

}
