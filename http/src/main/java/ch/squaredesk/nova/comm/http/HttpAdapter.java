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
import ch.squaredesk.nova.comm.rpc.RpcInvocation;
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
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

    public <ReplyMessageType extends MessageType> Single<ReplyMessageType> sendGetRequest(String destination) {
        return sendRequest(destination, null, new HttpSpecificInfo(HttpRequestMethod.GET), null, null );
    }

    public <ReplyMessageType extends MessageType>
    Single<ReplyMessageType> sendGetRequest(String destination, long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, null, new HttpSpecificInfo(HttpRequestMethod.GET), timeout, timeUnit);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
        Single<ReplyMessageType> sendPostRequest(String destination, RequestMessageType request) {
        return sendRequest(destination, request, new HttpSpecificInfo(HttpRequestMethod.POST), null, null);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
        Single<ReplyMessageType> sendPostRequest(String destination, RequestMessageType request, long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, request, new HttpSpecificInfo(HttpRequestMethod.POST), timeout, timeUnit );
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
        Single<ReplyMessageType> sendPutRequest(String destination, RequestMessageType request) {
        return sendRequest(destination, request, new HttpSpecificInfo(HttpRequestMethod.PUT), null, null);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
        Single<ReplyMessageType> sendPutRequest(String destination, RequestMessageType request, long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, request, new HttpSpecificInfo(HttpRequestMethod.PUT), timeout, timeUnit );
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
        Single<ReplyMessageType> sendRequest(String destination, RequestMessageType request) {
        return sendRequest(destination, request, null, null, null);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
        Single<ReplyMessageType> sendRequest(
                String destination,
                RequestMessageType request,
                long timeout,
                TimeUnit timeUnit) {
        return sendRequest(destination, request, null, timeout, timeUnit);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
        Single<ReplyMessageType> sendRequest(
                String destination,
                RequestMessageType request,
                HttpRequestMethod requestMethod) {
        return sendRequest(destination, request, new HttpSpecificInfo(requestMethod), null, null);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
        Single<ReplyMessageType> sendRequest(
                String destination,
                RequestMessageType request,
                HttpRequestMethod requestMethod,
                long timeout,
                TimeUnit timeUnit) {
        return sendRequest(destination, request, new HttpSpecificInfo(requestMethod), timeout, timeUnit);
    }

    public <RequestMessageType extends MessageType, ReplyMessageType extends MessageType>
        Single<ReplyMessageType> sendRequest (String destination, RequestMessageType request, HttpSpecificInfo httpInfo, Long timeout, TimeUnit timeUnit) {

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

        MessageSendingInfo<URL,HttpSpecificInfo> sendingInfo = new MessageSendingInfo.Builder<URL, HttpSpecificInfo>()
                .withTransportSpecificInfo(httpInfo)
                .withDestination(url)
                .build();

        return rpcClient.sendRequest(request, sendingInfo, timeout, timeUnit);
    }

    public Flowable<RpcInvocation<MessageType, MessageType, HttpSpecificInfo>> requests(String destination) {
        return rpcServer.requests(destination);
    }

    public void start() throws Exception {
        rpcServer.start();
    }

    public void shutdown() {
        rpcServer.shutdown();
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

        public Builder<MessageType> setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        protected void validate() {
            if (defaultRequestTimeout==null) {
                defaultRequestTimeout = 5L;
                defaultRequestTimeUnit = TimeUnit.SECONDS;
            }
        }

        public HttpAdapter<MessageType> createInstance() {
            validate();
            AsyncHttpClient httpClient = new AsyncHttpClient();
            rpcClient = new RpcClient<>(identifier, httpClient, messageMarshaller, messageUnmarshaller, metrics);
            if (httpServer == null) {
                logger.info("No httpServer provided, HTTP Adapter will only be usable in client mode!!!");
            } else {
                rpcServer = new RpcServer<>(identifier, httpServer, messageMarshaller, messageUnmarshaller, metrics);
            }
            return new HttpAdapter<>(this);
        }
    }

}
