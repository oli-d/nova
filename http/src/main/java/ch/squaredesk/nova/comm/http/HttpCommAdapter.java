/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.rpc.RpcInvocation;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class HttpCommAdapter<MessageType> {
    private final HttpRpcServer<MessageType> rpcServer;
    private final HttpRpcClient<MessageType> rpcClient;
    private final Long defaultRequestTimeout;
    private final TimeUnit defaultRequestTimeUnit;


    private HttpCommAdapter(Builder builder) {
        this.rpcServer = builder.rpcServer;
        this.rpcClient = builder.rpcClient;
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

    public Flowable<RpcInvocation<MessageType, MessageType, HttpSpecificInfo>> requests (String destination, BackpressureStrategy backpressureStrategy) {
        return rpcServer.requests(destination, backpressureStrategy);
    }

    public void start() throws Exception {
        rpcServer.start();
    }

    public void shutdown() {
        rpcServer.shutdown();
    }

    public static <MessageType> Builder<MessageType> builder() {
        return new Builder<>();
    }

    public static class Builder<MessageType> {
        private static Logger logger = LoggerFactory.getLogger(Builder.class);

        private String identifier;
        private Metrics metrics;
        private MessageMarshaller<MessageType,String> messageMarshaller;
        private MessageUnmarshaller<String,MessageType> messageUnmarshaller;
        private Function<Throwable, MessageType> errorReplyFactory;
        private HttpRpcServer<MessageType> rpcServer;
        private HttpRpcClient<MessageType> rpcClient;
        private Long defaultRequestTimeout;
        private TimeUnit defaultRequestTimeUnit;
        private Integer serverPort;

        private Builder() {
        }

        public Builder<MessageType> setDefaultRequestTimeout(long timeout, TimeUnit timeUnit) {
            requireNonNull(timeUnit);
            if (timeout>0) {
                defaultRequestTimeout = timeout;
                defaultRequestTimeUnit = timeUnit;
            }
            return this;
        }

        public Builder<MessageType> setServerPort(Integer port) {
            this.serverPort = port;
            return this;
        }

        public Builder<MessageType> setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder<MessageType> setMetrics(Metrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder<MessageType> setMessageMarshaller(MessageMarshaller<MessageType,String> marshaller) {
            this.messageMarshaller = marshaller;
            return this;
        }

        public Builder<MessageType> setMessageUnmarshaller(MessageUnmarshaller<String,MessageType> unmarshaller) {
            this.messageUnmarshaller = unmarshaller;
            return this;
        }

        public Builder<MessageType> setErrorReplyFactory(Function<Throwable, MessageType> errorReplyFactory) {
            this.errorReplyFactory = errorReplyFactory;
            return this;
        }

        public Builder<MessageType> validate() {
            requireNonNull(messageMarshaller," messageMarshaller instance must not be null");
            requireNonNull(messageUnmarshaller," messageUnmarshaller instance must not be null");
            requireNonNull(errorReplyFactory," errorReplyFactory instance must not be null");
            requireNonNull(metrics," Metrics instance must not be null");
            if (defaultRequestTimeout==null) {
                defaultRequestTimeout = 5L;
                defaultRequestTimeUnit = TimeUnit.SECONDS;
            }
            if (serverPort==null) {
                serverPort = 8080;
                logger.warn("No HTTP server port specified, falling back to default " + serverPort);
            }
            return this;
        }

        public HttpCommAdapter<MessageType> build() {
            validate();
            UrlInvoker urlInvoker = new UrlInvoker(identifier, metrics);
            rpcServer = new HttpRpcServer<>(identifier, serverPort, messageMarshaller, messageUnmarshaller, errorReplyFactory, metrics);
            rpcClient = new HttpRpcClient<>(identifier, urlInvoker, messageMarshaller, messageUnmarshaller, metrics);
            return new HttpCommAdapter<>(this);
        }
    }

}
