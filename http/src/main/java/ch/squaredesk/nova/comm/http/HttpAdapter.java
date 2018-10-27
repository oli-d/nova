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
import ch.squaredesk.nova.comm.DefaultMarshallerRegistryForStringAsTransportType;
import ch.squaredesk.nova.comm.MarshallerRegistry;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
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
        super(builder.marshallerRegistry, builder.metrics);
        this.rpcClient = builder.rpcClient;
        this.rpcServer = builder.rpcServer;
        this.defaultRequestTimeout = builder.defaultRequestTimeout;
        this.defaultRequestTimeUnit = builder.defaultRequestTimeUnit;
    }

    /*
    public <ReplyMessageType>
    Single<RpcReply<ReplyMessageType>> sendGetRequest(String destination) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET), null, null );
    }

    public <ReplyMessageType>
    Single<RpcReply<ReplyMessageType>> sendGetRequest(
                String destination,
                long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET), timeout, timeUnit);
    }

    public <RequestMessageType, ReplyMessageType>
    Single<RpcReply<ReplyMessageType>> sendPostRequest(
                String destination,
                RequestMessageType request) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST), null, null);
    }

    public <RequestMessageType, ReplyMessageType>
    Single<RpcReply<ReplyMessageType>> sendPostRequest(
                String destination,
                RequestMessageType request,
                long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST), timeout, timeUnit );
    }

    public <RequestMessageType, ReplyMessageType>
    Single<RpcReply<ReplyMessageType>> sendPutRequest(
                String destination,
                RequestMessageType request) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT), null, null);
    }

    public <RequestMessageType, ReplyMessageType>
    Single<RpcReply<ReplyMessageType>> sendPutRequest(
                String destination,
                RequestMessageType request,
                long timeout, TimeUnit timeUnit) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT), timeout, timeUnit );
    }

    public <RequestMessageType, ReplyMessageType>
    Single<RpcReply<ReplyMessageType>> sendRequest(
                String destination,
                RequestMessageType request) {
        return sendRequest(destination, request, null, null, null);
    }

    public <RequestMessageType, ReplyMessageType>
    Single<RpcReply<ReplyMessageType>> sendRequest(
                String destination,
                RequestMessageType request,
                long timeout,
                TimeUnit timeUnit) {
        return sendRequest(destination, request, null, timeout, timeUnit);
    }

    public <RequestMessageType, ReplyMessageType>
    Single<RpcReply<ReplyMessageType>> sendRequest(
                String destination,
                RequestMessageType request,
                HttpRequestMethod requestMethod) {
        return sendRequest(destination, request, new RequestInfo(requestMethod), null, null);
    }

*/
    public <RequestMessageType, ReplyMessageType> Single<RpcReply<ReplyMessageType>> sendRequest(
                String destination,
                RequestMessageType request,
                HttpRequestMethod requestMethod,
                Class<ReplyMessageType> replyType,
                long timeout,
                TimeUnit timeUnit) {
        return sendRequest(destination, request, new RequestInfo(requestMethod), replyType, timeout, timeUnit);
    }
    public <RequestMessageType, ReplyMessageType> Single<RpcReply<ReplyMessageType>> sendRequest (
                String destination,
                RequestMessageType request,
                RequestInfo httpInfo,
                Class<ReplyMessageType> replyType,
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

        return (Single<RpcReply<ReplyMessageType>>) rpcClient.sendRequest(request, sendingInfo, replyType, timeout, timeUnit);
    }


    public <RequestType> Flowable<RpcInvocation<RequestType>> requests(String destination, MessageUnmarshaller<String, RequestType> requestUnmarshaller) {
        return (Flowable<RpcInvocation<RequestType>>) rpcServer.requests(destination, requestUnmarshaller);
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
            MarshallerRegistry<String> marshallerRegistry = new DefaultMarshallerRegistryForStringAsTransportType();
            if (defaultRequestTimeout==null) {
                defaultRequestTimeout = 15L;
                defaultRequestTimeUnit = TimeUnit.SECONDS;
            }
            if (rpcClient == null) {
                AsyncHttpClient httpClient = new AsyncHttpClient();
                rpcClient = new RpcClient(identifier, httpClient, marshallerRegistry, metrics);
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
