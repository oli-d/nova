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

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class RpcClient extends ch.squaredesk.nova.comm.rpc.RpcClient<String, RequestMessageMetaData, ReplyMessageMetaData> {
    private final AsyncHttpClient client;

    protected RpcClient(String identifier,
                        AsyncHttpClient client,
                        Metrics metrics) {
        super(identifier, metrics);
        this.client = client;
    }

    @Override
    public <T, U> Single<RpcReply<U>> sendRequest(
            T request,
            RequestMessageMetaData requestMessageMetaData,
            Class<U> expectedReplyType,
            MessageTranscriber<String> messageTranscriber,
            long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");

        String requestAsString;
        try {
            requestAsString = request != null ? messageTranscriber.transcribeOutgoingMessage(request) : null;
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
            return new RpcReply<>(messageTranscriber.transcribeIncomingMessage(responseBody, expectedReplyType), metaData);
        });

        return resultSingle.timeout(timeout, timeUnit);
    }

    void shutdown() {
        client.close();
    }

}
