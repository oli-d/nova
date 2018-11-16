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

import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class RpcClient extends ch.squaredesk.nova.comm.rpc.RpcClient<String, RequestMessageMetaData, ReplyMessageMetaData> {
    private final AsyncHttpClient client;
    private Map<String, String> standardHeadersForAllRequests;
    private boolean contentTypeInStandardHeaders;

    RpcClient(String identifier,
                        AsyncHttpClient client,
                        Metrics metrics) {
        super(identifier, metrics);
        this.client = client;
    }

    @Override
    public <T, U> Single<RpcReply<U>> sendRequest(
            T request,
            RequestMessageMetaData requestMessageMetaData,
            Function<T, String> requestTranscriber,
            Function<String, U> replyTranscriber,
            long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");

        String requestAsString;
        try {
            requestAsString = request != null ? requestTranscriber.apply(request) : null;
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

        addHeadersToRequest(standardHeadersForAllRequests, requestBuilder);
        addHeadersToRequest(requestMessageMetaData.details.headers, requestBuilder);
        if (!contentTypeInStandardHeaders && !headersContainContentType(requestMessageMetaData.details.headers)) {
            requestBuilder.addHeader("Content-Type", "application/json; charset=utf-8");
        }

        ListenableFuture<Response> resultFuture = requestBuilder.execute();

        Single<RpcReply<U>> resultSingle = Single.fromFuture(resultFuture).map(response -> {
            ReplyMessageMetaData metaData = createMetaDataFromReply(requestMessageMetaData, response);
            String responseBody = response.getResponseBody();
            U replyObject = responseBody == null || responseBody.trim().isEmpty() ? null : replyTranscriber.apply(responseBody);
            metricsCollector.rpcCompleted(requestMessageMetaData.destination, responseBody);
            return new RpcReply<>(replyObject, metaData);
        });

        return resultSingle.timeout(timeout, timeUnit);
    }

    private static ReplyMessageMetaData createMetaDataFromReply(RequestMessageMetaData requestMessageMetaData, Response response) {
        Map<String, String> headersToReturn;
        FluentCaseInsensitiveStringsMap responseHeaders = response.getHeaders();
        if (responseHeaders.isEmpty()) {
            headersToReturn = Collections.emptyMap();
        } else {
            Map<String, String> headerMap = new HashMap<>(responseHeaders.size() + 1, 1.0f);
            responseHeaders.forEach((key, valueList) -> {
                headerMap.put(key, String.join(",", valueList));
            });
            headersToReturn = headerMap;
        }
        return new ReplyMessageMetaData(
                        requestMessageMetaData.destination,
                        new ReplyInfo(response.getStatusCode(), headersToReturn));
    }

    void shutdown() {
        client.close();
    }

    public Map<String, String> getStandardHeadersForAllRequests() {
        return standardHeadersForAllRequests;
    }

    public void setStandardHeadersForAllRequests(Map<String, String> standardHeadersForAllRequests) {
        this.standardHeadersForAllRequests = standardHeadersForAllRequests;
        this.contentTypeInStandardHeaders = headersContainContentType(standardHeadersForAllRequests);
    }

    private static boolean headersContainContentType (Map<String, String> headersToCheck) {
        return headersToCheck!= null && headersToCheck.containsKey("Content-Type");
    }

    private static void addHeadersToRequest (Map<String, String> headersToAdd, AsyncHttpClient.BoundRequestBuilder requestBuilder) {
        if (headersToAdd != null) {
            headersToAdd.entrySet().forEach(entry -> requestBuilder.addHeader(entry.getKey(), entry.getValue()));
        }
    }
}
