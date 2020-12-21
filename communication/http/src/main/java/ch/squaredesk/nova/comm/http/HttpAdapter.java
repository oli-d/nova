/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.CommAdapter;
import ch.squaredesk.nova.comm.CommAdapterBuilder;
import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;
import com.ning.http.client.AsyncHttpClient;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class HttpAdapter extends CommAdapter<String> implements HttpServerInstanceListener, HttpClientInstanceListener {
    private final RpcClient rpcClient;
    private final RpcServer rpcServer;
    private final Duration defaultRequestTimeout;


    private HttpAdapter(Builder builder) {
        super(builder.getMessageTranscriber(), builder.getMetrics());
        this.rpcClient = builder.rpcClient;
        this.rpcServer = builder.rpcServer;
        this.defaultRequestTimeout = builder.defaultRequestTimeout;
    }

    public boolean isServerStarted () {
        return rpcServer != null && rpcServer.isStarted();
    }
    public <T> FrozenHttpAdapter<T> freeze (Class<T> typeClass) {
        return new FrozenHttpAdapter<>(this, typeClass);
    }

    public void setStandardHeadersForAllRequests (Map<String, String> standardHeaders) {
        Optional.ofNullable(rpcClient).ifPresent(client -> client.setStandardHeadersForAllRequests(standardHeaders));
    }

    public Optional<Map<String, String>> getStandardHeadersForAllRequests () {
        return Optional.ofNullable(rpcClient).map(RpcClient::getStandardHeadersForAllRequests);
    }

    /////////
    /////////
    ///////// The client side
    /////////
    /////////

    /////////
    /////////////////// GET convenience methods
    /////////
    public <U> Single<RpcReply<U>> sendGetRequest(String destination, Class<U> replyType) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET), replyType, null );
    }

    public <U> Single<RpcReply<U>> sendGetRequest(String destination, Map<String, String> headers, Class<U> replyType) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET, headers), replyType, null );
    }

    public <U> Single<RpcReply<U>> sendGetRequest(
                String destination,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET), replyType, timeout);
    }

    public <U> Single<RpcReply<U>> sendGetRequest(
                String destination,
                Map<String, String> headers,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET, headers), replyType, timeout);
    }

    public <U> Single<RpcReply<U>> sendGetRequest(String destination, Function<String, U> replyTranscriber) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET), replyTranscriber, null );
    }

    public <U> Single<RpcReply<U>> sendGetRequest(String destination, Map<String, String> headers, Function<String, U> replyTranscriber) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET, headers), replyTranscriber, null );
    }

    public <U> Single<RpcReply<U>> sendGetRequest(
                String destination,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET), replyTranscriber, timeout);
    }

    public <U> Single<RpcReply<U>> sendGetRequest(
                String destination,
                Map<String, String> headers,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, null, new RequestInfo(HttpRequestMethod.GET, headers), replyTranscriber, timeout);
    }

    public Single<RpcReply<InputStream>> sendGetRequestAndRetrieveResponseAsStream(String destination) {
        return sendRequestAndRetrieveResponseAsStream(destination, null, new RequestInfo(HttpRequestMethod.GET), null );
    }

    public Single<RpcReply<InputStream>> sendGetRequestAndRetrieveResponseAsStream(String destination, Map<String, String> headers) {
        return sendRequestAndRetrieveResponseAsStream(destination, null, new RequestInfo(HttpRequestMethod.GET, headers), null );
    }

    public Single<RpcReply<InputStream>> sendGetRequestAndRetrieveResponseAsStream(
            String destination,
            Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, null, new RequestInfo(HttpRequestMethod.GET), timeout);
    }

    public Single<RpcReply<InputStream>> sendGetRequestAndRetrieveResponseAsStream(
            String destination,
            Map<String, String> headers,
            Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, null, new RequestInfo(HttpRequestMethod.GET, headers), timeout);
    }

    /////////
    /////////////////// POST convenience methods
    /////////
    public <T, U> Single<RpcReply<U>> sendPostRequest(
                String destination,
                T request,
                Class<U> replyType) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST), replyType, null);
    }

    public <T, U> Single<RpcReply<U>> sendPostRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Class<U> replyType) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST, headers), replyType, null);
    }

    public <T, U> Single<RpcReply<U>> sendPostRequest(
                String destination,
                T request,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST), replyType, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendPostRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST, headers), replyType, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendPostRequest(
                String destination,
                T request,
                Function<String, U> replyTranscriber) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST), replyTranscriber, null);
    }

    public <T, U> Single<RpcReply<U>> sendPostRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Function<String, U> replyTranscriber) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST, headers), replyTranscriber, null);
    }

    public <T, U> Single<RpcReply<U>> sendPostRequest(
                String destination,
                T request,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST), replyTranscriber, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendPostRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.POST, headers), replyTranscriber, timeout );
    }

    public <T> Single<RpcReply<InputStream>> sendPostRequestAndRetrieveResponseAsStream(
            String destination,
            T request) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.POST),  null);
    }

    public <T> Single<RpcReply<InputStream>> sendPostRequestAndRetrieveResponseAsStream(
            String destination,
            T request,
            Map<String, String> headers) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.POST, headers),  null);
    }

    public <T> Single<RpcReply<InputStream>> sendPostRequestAndRetrieveResponseAsStream(
            String destination,
            T request,
            Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.POST),  timeout );
    }

    public <T> Single<RpcReply<InputStream>> sendPostRequestAndRetrieveResponseAsStream(
            String destination,
            T request,
            Map<String, String> headers,
            Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.POST, headers),  timeout );
    }

    /////////
    /////////////////// PUT convenience methods
    /////////
    public <T, U> Single<RpcReply<U>> sendPutRequest(
                String destination,
                T request,
                Class<U> replyType) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT), replyType, null);
    }

    public <T, U> Single<RpcReply<U>> sendPutRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Class<U> replyType) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT, headers), replyType, null);
    }

    public <T, U> Single<RpcReply<U>> sendPutRequest(
                String destination,
                T request,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT), replyType, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendPutRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT, headers), replyType, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendPutRequest(
                String destination,
                T request,
                Function<String, U> replyTranscriber) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT), replyTranscriber, null);
    }

    public <T, U> Single<RpcReply<U>> sendPutRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Function<String, U> replyTranscriber) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT, headers), replyTranscriber, null);
    }

    public <T, U> Single<RpcReply<U>> sendPutRequest(
                String destination,
                T request,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT), replyTranscriber, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendPutRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PUT, headers), replyTranscriber, timeout );
    }

    public <T> Single<RpcReply<InputStream>> sendPutRequestAndRetrieveResponseAsStream(
                String destination,
                T request) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.PUT),  null);
    }

    public <T> Single<RpcReply<InputStream>> sendPutRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                Map<String, String> headers) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.PUT, headers),  null);
    }

    public <T> Single<RpcReply<InputStream>> sendPutRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.PUT),  timeout );
    }

    public <T> Single<RpcReply<InputStream>> sendPutRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                Map<String, String> headers,
                Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.PUT, headers),  timeout );
    }

    /////////
    /////////////////// PATCH convenience methods
    /////////
    public <T, U> Single<RpcReply<U>> sendPatchRequest(
                String destination,
                T request,
                Class<U> replyType) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PATCH), replyType, null);
    }

    public <T, U> Single<RpcReply<U>> sendPatchRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Class<U> replyType) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PATCH, headers), replyType, null);
    }

    public <T, U> Single<RpcReply<U>> sendPatchRequest(
                String destination,
                T request,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PATCH), replyType, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendPatchRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PATCH, headers), replyType, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendPatchRequest(
                String destination,
                T request,
                Function<String, U> replyTranscriber) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PATCH), replyTranscriber, null);
    }

    public <T, U> Single<RpcReply<U>> sendPatchRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Function<String, U> replyTranscriber) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PATCH, headers), replyTranscriber, null);
    }

    public <T, U> Single<RpcReply<U>> sendPatchRequest(
                String destination,
                T request,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PATCH), replyTranscriber, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendPatchRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.PATCH, headers), replyTranscriber, timeout );
    }

    public <T> Single<RpcReply<InputStream>> sendPatchRequestAndRetrieveResponseAsStream(
                String destination,
                T request) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.PATCH), null);
    }

    public <T> Single<RpcReply<InputStream>> sendPatchRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                Map<String, String> headers) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.PATCH, headers),  null);
    }

    public <T> Single<RpcReply<InputStream>> sendPatchRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.PATCH), timeout );
    }

    public <T> Single<RpcReply<InputStream>> sendPatchRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                Map<String, String> headers,
                Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.PATCH, headers), timeout );
    }

    /////////
    /////////////////// DELETE convenience methods
    /////////
    public <T, U> Single<RpcReply<U>> sendDeleteRequest(
                String destination,
                T request,
                Class<U> replyType) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.DELETE), replyType, null);
    }

    public <T, U> Single<RpcReply<U>> sendDeleteRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Class<U> replyType) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.DELETE, headers), replyType, null);
    }

    public <T, U> Single<RpcReply<U>> sendDeleteRequest(
                String destination,
                T request,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.DELETE), replyType, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendDeleteRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.DELETE, headers), replyType, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendDeleteRequest(
                String destination,
                T request,
                Function<String, U> replyTranscriber) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.DELETE), replyTranscriber, null);
    }

    public <T, U> Single<RpcReply<U>> sendDeleteRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Function<String, U> replyTranscriber) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.DELETE, headers), replyTranscriber, null);
    }

    public <T, U> Single<RpcReply<U>> sendDeleteRequest(
                String destination,
                T request,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.DELETE), replyTranscriber, timeout );
    }

    public <T, U> Single<RpcReply<U>> sendDeleteRequest(
                String destination,
                T request,
                Map<String, String> headers,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(HttpRequestMethod.DELETE, headers), replyTranscriber, timeout );
    }

    public <T> Single<RpcReply<InputStream>> sendDeleteRequestAndRetrieveResponseAsStream(
                String destination,
                T request) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.DELETE), null);
    }

    public <T> Single<RpcReply<InputStream>> sendDeleteRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                Map<String, String> headers) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.DELETE, headers), null);
    }

    public <T> Single<RpcReply<InputStream>> sendDeleteRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.DELETE), timeout );
    }

    public <T> Single<RpcReply<InputStream>> sendDeleteRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                Map<String, String> headers,
                Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(HttpRequestMethod.DELETE, headers), timeout );
    }

    /////////
    /////////////////// other convenience methods
    /////////
    public <T, U> Single<RpcReply<U>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Class<U>replyType) {
        return sendRequest(destination, request, new RequestInfo(requestMethod), replyType, null);
    }

    public <T, U> Single<RpcReply<U>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Map<String, String> headers,
                Class<U>replyType) {
        return sendRequest(destination, request, new RequestInfo(requestMethod, headers), replyType, null);
    }


    public <T, U> Single<RpcReply<U>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(requestMethod), replyType, timeout);
    }

    public <T, U> Single<RpcReply<U>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Map<String, String> headers,
                Class<U> replyType,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(requestMethod, headers), replyType, timeout);
    }

    public <T, U> Single<RpcReply<U>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Function<String, U> replyTranscriber) {
        return sendRequest(destination, request, new RequestInfo(requestMethod), replyTranscriber, null);
    }

    public <T, U> Single<RpcReply<U>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Map<String, String> headers,
                Function<String, U> replyTranscriber) {
        return sendRequest(destination, request, new RequestInfo(requestMethod, headers), replyTranscriber, null);
    }

    public <T, U> Single<RpcReply<U>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(requestMethod), replyTranscriber, timeout);
    }

    public <T, U> Single<RpcReply<U>> sendRequest(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Map<String, String> headers,
                Function<String, U> replyTranscriber,
                Duration timeout) {
        return sendRequest(destination, request, new RequestInfo(requestMethod, headers), replyTranscriber, timeout);
    }

    public <T> Single<RpcReply<InputStream>> sendRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                HttpRequestMethod requestMethod) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(requestMethod),  null);
    }

    public <T> Single<RpcReply<InputStream>> sendRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Map<String, String> headers) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(requestMethod, headers), null);
    }

    public <T> Single<RpcReply<InputStream>> sendRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(requestMethod), timeout);
    }

    public <T> Single<RpcReply<InputStream>> sendRequestAndRetrieveResponseAsStream(
                String destination,
                T request,
                HttpRequestMethod requestMethod,
                Map<String, String> headers,
                Duration timeout) {
        return sendRequestAndRetrieveResponseAsStream(destination, request, new RequestInfo(requestMethod, headers), timeout);
    }

    public <T, U> Single<RpcReply<U>> sendRequest (
                String destination,
                T request,
                RequestInfo sendingInfo,
                Class<U> replyType,
                Duration timeout) {

        return sendRequest(
                destination,
                request,
                sendingInfo,
                messageTranscriber.getIncomingMessageTranscriber(replyType),
                timeout);
    }

    /////////
    /////////
    /////////////////// finally, the implementation
    /////////
    /////////
    public <T, U> Single<RpcReply<U>> sendRequest (
                String destination,
                T request,
                RequestInfo httpInfo,
                Function<String, U> replyTranscriber,
                Duration timeout) {

        return doSendRequest(
                destination,
                httpInfo,
                (sendingInfo, theTimeout) ->
                        rpcClient.sendRequest(
                                request,
                                sendingInfo,
                                messageTranscriber.getOutgoingMessageTranscriber(request),
                                replyTranscriber,
                                theTimeout),
                timeout);
    }

    public <T> Single<RpcReply<InputStream>> sendRequestAndRetrieveResponseAsStream (
                String destination,
                T request,
                RequestInfo httpInfo,
                Duration timeout) {

        return doSendRequest(
                destination,
                httpInfo,
                (sendingInfo, theTimeout) ->
                        rpcClient.sendRequestAndRetrieveResponseAsStream(
                                request,
                                sendingInfo,
                                messageTranscriber.getOutgoingMessageTranscriber(request),
                                theTimeout),
                timeout);
    }

    @FunctionalInterface
    public interface RequestSender<T> {
        Single<RpcReply<T>> apply (RequestMessageMetaData metaData, Duration timeout);
    }

    private <T> Single<RpcReply<T>> doSendRequest (
                String destination,
                RequestInfo httpInfo,
                RequestSender<T> requestSender,
                Duration timeout) {


        URL url;
        try {
            url = new URL(destination);
        } catch (MalformedURLException e) {
            return Single.error(new IllegalArgumentException("Invalid URL format " + destination,e));
        }

        RequestMessageMetaData sendingInfo = new RequestMessageMetaData(url, httpInfo);

        try {
            return requestSender.apply(sendingInfo, Optional.ofNullable(timeout).orElse(defaultRequestTimeout));
        } catch (Exception e) {
            return Single.error(e);
        }
    }

    /////////
    /////////
    ///////// The server side
    /////////
    /////////
    public <T> Flowable<RpcInvocation<T>> requests(String destination, Class<T> requestType) {
        return rpcServer.requests(destination, requestType);
    }

    public void start() throws Exception {
        rpcServer.start();
    }

    public void shutdown() {
        if (rpcServer!=null) {
            rpcServer.shutdown();
        }
        if (rpcClient!=null) {
            rpcClient.shutdown();
        }
    }


    @Override
    public void httpServerInstanceCreated(HttpServer httpServer) {
        if (this.rpcServer != null) {
            this.rpcServer.httpServerInstanceCreated(httpServer);
        }
    }

    @Override
    public void httpClientInstanceCreated(AsyncHttpClient httpClient) {
        if (this.rpcClient != null) {
            this.rpcClient.httpClientInstanceCreated(httpClient);
        }
    }

    /////////
    /////////
    ///////// The builder
    /////////
    /////////
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends CommAdapterBuilder<String, HttpAdapter> {
        private static Logger logger = LoggerFactory.getLogger(Builder.class);

        private String identifier;
        private AsyncHttpClient httpClient;
        private RpcClient rpcClient;
        private RpcServer rpcServer;
        private HttpServer httpServer;
        private Duration defaultRequestTimeout;

        private Builder() {
        }

        public Builder setDefaultRequestTimeout(Duration timeout) {
            this.defaultRequestTimeout = requireNonNull(timeout);
            return this;
        }

        public Builder setHttpClient(AsyncHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder setHttpServer(HttpServer httpServer) {
            this.httpServer = httpServer;
            return this;
        }

        @Override
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

        @Override
        protected void validate() {
            // nothing to validate
        }

        protected HttpAdapter createInstance() {
            validate();
            if (messageTranscriber == null) {
                messageTranscriber = new DefaultMessageTranscriberForStringAsTransportType();
            }
            if (defaultRequestTimeout==null || defaultRequestTimeout.toMillis() <= 0) {
                defaultRequestTimeout = Duration.ofSeconds(15);
            }
            if (rpcClient == null) {
                if (httpClient == null) {
                    logger.info("No httpClient provided (yet), HTTP Adapter will not be usable in client mode until HttpClient becomes available!!!");
                }
                rpcClient = new RpcClient(identifier, httpClient, metrics);
            }
            if (rpcServer == null) {
                if (httpServer == null) {
                    logger.info("No httpServer provided (yet), HTTP Adapter will not be usable in server mode until HttpServer becomes available!!!");
                }
                rpcServer = new RpcServer(identifier, httpServer, messageTranscriber, metrics);
            }
            return new HttpAdapter(this);
        }
    }

}
