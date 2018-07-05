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

import ch.squaredesk.nova.comm.CommAdapterBuilder;
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

public class HttpAdapter<MessageType> {
    private final RpcClient<MessageType> rpcClient;
    private final RpcServer<MessageType> rpcServer;
    private final Long defaultRequestTimeout;
    private final TimeUnit defaultRequestTimeUnit;


    private HttpAdapter(Builder<MessageType> builder) {
        this.rpcClient = builder.rpcClient;
        this.rpcServer = builder.rpcServer;
        this.defaultRequestTimeout = builder.defaultRequestTimeout;
        this.defaultRequestTimeUnit = builder.defaultRequestTimeUnit;
    }

    public <ReplyMessageType extends MessageType>
    Single<RpcReply<ReplyMessageType>> sendGetRequest(String destination) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET), null, null );
    }

    public <ReplyMessageType extends MessageType>
    Single<RpcReply<ReplyMessageType>> sendGetRequest(
                String destination,
                long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET), timeout, timeUnit);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
    Single<RpcReply<ReplyMessageType>> sendPostRequest(
                String destination,
                RequestMessageType request) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST), null, null);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
    Single<RpcReply<ReplyMessageType>> sendPostRequest(
                String destination,
                RequestMessageType request,
                long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST), timeout, timeUnit );
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
    Single<RpcReply<ReplyMessageType>> sendPutRequest(
                String destination,
                RequestMessageType request) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT), null, null);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
    Single<RpcReply<ReplyMessageType>> sendPutRequest(
                String destination,
                RequestMessageType request,
                long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT), timeout, timeUnit );
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
    Single<RpcReply<ReplyMessageType>> sendRequest(
                String destination,
                RequestMessageType request) {
        return sendRequest(destination, request, null, null, null);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
    Single<RpcReply<ReplyMessageType>> sendRequest(
                String destination,
                RequestMessageType request,
                long timeout,
                TimeUnit timeUnit) {
        return sendRequest(destination, request, null, timeout, timeUnit);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
    Single<RpcReply<ReplyMessageType>> sendRequest(
                String destination,
                RequestMessageType request,
                HttpRequestMethod requestMethod) {
        return sendRequest(destination, request, new RequestInfo(requestMethod), null, null);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
    Single<RpcReply<ReplyMessageType>> sendRequest(
                String destination,
                RequestMessageType request,
                HttpRequestMethod requestMethod,
                long timeout,
                TimeUnit timeUnit) {
        return sendRequest(destination, request, new RequestInfo(requestMethod), timeout, timeUnit);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
    Single<RpcReply<ReplyMessageType>> sendRequest (
                String destination,
                RequestMessageType request,
                RequestInfo httpInfo,
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

        return rpcClient.sendRequest(request, sendingInfo, timeout, timeUnit);
    }

    public Flowable<RpcInvocation<MessageType>> requests(String destination) {
        return rpcServer.requests(destination);
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

    public static <MessageType> Builder<MessageType> builder(Class<MessageType> messageTypeClass) {
        return new Builder<>(messageTypeClass);
    }

    public static class Builder<MessageType> extends CommAdapterBuilder<MessageType, HttpAdapter<MessageType>>{
        private static Logger logger = LoggerFactory.getLogger(Builder.class);

        private String identifier;
        private HttpServer httpServer;
        private RpcClient<MessageType> rpcClient;
        private RpcServer<MessageType> rpcServer;
        private Long defaultRequestTimeout;
        private TimeUnit defaultRequestTimeUnit;

        private Builder(Class<MessageType> messageTypeClass) {
            super(messageTypeClass);
        }

        public Builder<MessageType> setDefaultRequestTimeout(long timeout, TimeUnit timeUnit) {
            requireNonNull(timeUnit);
            if (timeout>0) {
                defaultRequestTimeout = timeout;
                defaultRequestTimeUnit = timeUnit;
            }
            return this;
        }

        public Builder<MessageType> setHttpServer(HttpServer httpServer) {
            this.httpServer = httpServer;
            return this;
        }

        public Builder<MessageType> setRpcServer(RpcServer<MessageType> rpcServer) {
            this.rpcServer = rpcServer;
            return this;
        }

        public Builder<MessageType> setRpcClient(RpcClient<MessageType> rpcClient) {
            this.rpcClient = rpcClient;
            return this;
        }

        public Builder<MessageType> setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        protected void validate() {
        }

        protected HttpAdapter<MessageType> createInstance() {
            validate();
            if (defaultRequestTimeout==null) {
                defaultRequestTimeout = 15L;
                defaultRequestTimeUnit = TimeUnit.SECONDS;
            }
            if (rpcClient == null) {
                AsyncHttpClient httpClient = new AsyncHttpClient();
                rpcClient = new RpcClient<>(identifier, httpClient, messageMarshaller, messageUnmarshaller, metrics);
            }
            if (rpcServer == null) {
                if (httpServer == null) {
                    logger.info("No httpServer provided, HTTP Adapter will only be usable in client mode!!!");
                } else {
                    rpcServer = new RpcServer<>(identifier, httpServer, messageMarshaller, messageUnmarshaller, metrics);
                }
            }
            return new HttpAdapter<>(this);
        }
    }

}
