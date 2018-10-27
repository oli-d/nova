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

import ch.squaredesk.nova.comm.DefaultMarshallerRegistryForStringAsTransportType;
import ch.squaredesk.nova.comm.MarshallerRegistry;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class RpcClient extends ch.squaredesk.nova.comm.rpc.RpcClient<String, RequestMessageMetaData, ReplyMessageMetaData> {
    private final AsyncHttpClient client;
    private final MarshallerRegistry<String> marshallerRegistry;

    protected RpcClient(String identifier,
                        AsyncHttpClient client,
                        Metrics metrics) {
        this(identifier, client, new DefaultMarshallerRegistryForStringAsTransportType(), metrics);
    }

    protected RpcClient(String identifier,
                        AsyncHttpClient client,
                        MarshallerRegistry<String> marshallerRegistry,
                        Metrics metrics) {
        super(identifier, metrics);
        this.client = client;
        this.marshallerRegistry = marshallerRegistry;
    }

    public <RequestType, ReplyType> Single<RpcReply<ReplyType>> sendRequest(
            RequestType request, RequestMessageMetaData requestMessageMetaData,
            Class<ReplyType> replyType,
            long timeout, TimeUnit timeUnit) {

        MessageMarshaller<RequestType, String> requestMarshaller = determineMarshallerFor(request);
        MessageUnmarshaller<String, ReplyType> replyUnmarshaller = determineUnmarshallerFor(replyType);

        return sendRequest(request, requestMessageMetaData, requestMarshaller, replyUnmarshaller, timeout, timeUnit);
    }

    private <T> MessageMarshaller<T, String> determineMarshallerFor (T request) {
        MessageMarshaller<T, String> requestMarshaller = null;
        if (request != null) {
            requestMarshaller = null;
            if (marshallerRegistry != null) {
                requestMarshaller = marshallerRegistry.getMarshallerForMessageType((Class<T>) request.getClass());
            }
            if (requestMarshaller == null) {
                throw new IllegalArgumentException("Unable to find marshaller for type " + request.getClass());
            }
        }
        return requestMarshaller;
    }

    private <T> MessageUnmarshaller<String, T> determineUnmarshallerFor (Class<T> replyType) {
        MessageUnmarshaller<String, T> replyUnmarshaller = marshallerRegistry.getUnmarshallerForMessageType(replyType);
        if (replyUnmarshaller == null) {
            throw new IllegalArgumentException("Unable to find unmarshaller for reply type " + replyType);
        }
        return replyUnmarshaller;
    }

    @Override
    public <T, U> Single<RpcReply<U>> sendRequest(
            T request,
            RequestMessageMetaData requestMessageMetaData,
            MessageMarshaller<T, String> requestMarshaller,
            MessageUnmarshaller<String, U> replyUnmarshaller, long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");

        String requestAsString;
        try {
            requestAsString = request != null ? requestMarshaller.marshal(request) : null;
        } catch (Exception e) {
            return Single.error(e);
        }

        AsyncHttpClient.BoundRequestBuilder requestBuilder;
        if (requestMessageMetaData.details.requestMethod == HttpRequestMethod.POST) {
            requestBuilder = client.preparePost(requestMessageMetaData.destination.toString()).setBody(requestAsString);
        } else if (requestMessageMetaData.details.requestMethod == HttpRequestMethod.PUT) {
            requestBuilder = client.preparePut(requestMessageMetaData.destination.toString()).setBody(requestAsString);
        } else if (requestMessageMetaData.details.requestMethod == HttpRequestMethod.DELETE) {
            requestBuilder = client.prepareDelete(requestMessageMetaData.destination.toString()).setBody(requestAsString);
        } else {
            requestBuilder = client.prepareGet(requestMessageMetaData.destination.toString());
        }

        ListenableFuture<Response> resultFuture = requestBuilder
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .execute();

        Single<RpcReply<U>> resultSingle = Single.fromFuture(resultFuture).map(response -> {
            int statusCode = response.getStatusCode();
            ReplyMessageMetaData metaData = new ReplyMessageMetaData(
                    requestMessageMetaData.destination,
                    new ReplyInfo(statusCode));
            String responseBody = response.getResponseBody();
            metricsCollector.rpcCompleted(requestMessageMetaData.destination, responseBody);
            return new RpcReply<>(replyUnmarshaller.unmarshal(responseBody), metaData);
        });

        return resultSingle.timeout(timeout, timeUnit);
    }

    void shutdown() {
        client.close();
    }

}
