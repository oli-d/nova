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
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class RpcClient<InternalMessageType> extends ch.squaredesk.nova.comm.rpc.RpcClient<InternalMessageType, RequestMessageMetaData, ReplyMessageMetaData> {
    private final AsyncHttpClient client;
    private final MessageMarshaller<InternalMessageType, String> messageMarshaller;
    private final MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller;

    protected RpcClient(String identifier,
                        AsyncHttpClient client,
                        MessageMarshaller<InternalMessageType, String> messageMarshaller,
                        MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                        Metrics metrics) {
        super(identifier, metrics);
        this.client = client;
        this.messageUnmarshaller = messageUnmarshaller;
        this.messageMarshaller = messageMarshaller;
    }


    public <ReplyType extends InternalMessageType> Single<RpcReply<ReplyType>> sendRequest(
            InternalMessageType request,
            RequestMessageMetaData requestMessageMetaData,
            long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");

        String requestAsString;
        try {
            requestAsString = request != null ? messageMarshaller.marshal(request) : null;
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

        if (requestMessageMetaData.details.headerParams == null || 
            requestMessageMetaData.details.headerParams.isEmpty()) {
            requestBuilder.addHeader("Content-Type", "application/json; charset=utf-8");
        } else {
            requestMessageMetaData.details.headerParams.forEach((key, value) -> requestBuilder.addHeader(key, value));
        }
            
        ListenableFuture<Response> resultFuture = requestBuilder.execute();

        Single<RpcReply<ReplyType>> resultSingle = Single.fromFuture(resultFuture).map(response -> {
            int statusCode = response.getStatusCode();
            ReplyMessageMetaData metaData = new ReplyMessageMetaData(
                    requestMessageMetaData.destination,
                    new ReplyInfo(statusCode));
            String responseBody = response.getResponseBody();
            metricsCollector.rpcCompleted(requestMessageMetaData.destination, responseBody);
            return new RpcReply<>((ReplyType) messageUnmarshaller.unmarshal(responseBody), metaData);
        });

        return resultSingle.timeout(timeout, timeUnit);
    }

    void shutdown() {
        client.close();
    }

}
