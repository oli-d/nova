/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class RpcClient extends ch.squaredesk.nova.comm.rpc.RpcClient<String, RequestMessageMetaData, ReplyMessageMetaData>
                        implements HttpClientInstanceListener {
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private AsyncHttpClient client;
    private Map<String, String> standardHeadersForAllRequests;
    private boolean contentTypeInStandardHeaders;

    protected RpcClient(String identifier,
                        AsyncHttpClient client,
                        Metrics metrics) {
        super(Metrics.name("http", identifier), metrics);
        this.client = client;
    }

    @Override
    public <T, U> Single<RpcReply<U>> sendRequest(
            T request,
            RequestMessageMetaData requestMessageMetaData,
            Function<T, String> requestTranscriber,
            Function<String, U> replyTranscriber,
            long timeout, TimeUnit timeUnit) {

        return doRequest(
                request, requestMessageMetaData, requestTranscriber,
                response -> {
                    String responseBody = response.getResponseBody();
                    return responseBody == null || responseBody.trim().isEmpty() ? null : replyTranscriber.apply(responseBody);
                },
                timeout, timeUnit
        );
    }

    public <T> Single<RpcReply<InputStream>> sendRequestAndRetrieveResponseAsStream(
            T request,
            RequestMessageMetaData requestMessageMetaData,
            Function<T, String> requestTranscriber,
            long timeout, TimeUnit timeUnit) {
        return doRequest(
                request, requestMessageMetaData, requestTranscriber,
                Response::getResponseBodyAsStream,
                timeout, timeUnit
        );
    }


    private <T, U> Single<RpcReply<U>> doRequest(T request,
                                 RequestMessageMetaData requestMessageMetaData,
                                 Function<T, String> requestTranscriber,
                                 Function<Response, U> responseMapper,
                                 long timeout, TimeUnit timeUnit) {
        String requestAsString;
        try {
            requestAsString = request != null ? requestTranscriber.apply(request) : null;
        } catch (Exception e) {
            return Single.error(e);
        }

        AsyncHttpClient.BoundRequestBuilder requestBuilder = createRequestBuilder(requestAsString, requestMessageMetaData, timeout, timeUnit);

        return Single.fromCallable(() -> requestBuilder.execute().get())
                .map(response -> {
                    ReplyMessageMetaData metaData = createMetaDataFromReply(requestMessageMetaData, response);
                    U result = responseMapper.apply(response);
                    metricsCollector.rpcCompleted(MetricsCollectorInfoCreator.createInfoFor(requestMessageMetaData.destination), result);
                    return new RpcReply<>(result, metaData);
                })
                .timeout(timeout, timeUnit);
    }

    private AsyncHttpClient.BoundRequestBuilder createRequestBuilder(String requestAsString, RequestMessageMetaData requestMessageMetaData, long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");

        AsyncHttpClient.BoundRequestBuilder requestBuilder;
        if (requestMessageMetaData.details.requestMethod == HttpRequestMethod.POST) {
            requestBuilder = client.preparePost(requestMessageMetaData.destination.toString()).setBody(requestAsString);
        } else if (requestMessageMetaData.details.requestMethod == HttpRequestMethod.PUT) {
            requestBuilder = client.preparePut(requestMessageMetaData.destination.toString()).setBody(requestAsString);
        } else if (requestMessageMetaData.details.requestMethod == HttpRequestMethod.PATCH) {
            requestBuilder = client.preparePatch(requestMessageMetaData.destination.toString()).setBody(requestAsString);
        } else if (requestMessageMetaData.details.requestMethod == HttpRequestMethod.OPTIONS) {
            requestBuilder = client.prepareOptions(requestMessageMetaData.destination.toString()).setBody(requestAsString);
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

        return requestBuilder.setRequestTimeout((int)timeUnit.toMillis(timeout));
    }

    private static ReplyMessageMetaData createMetaDataFromReply(RequestMessageMetaData requestMessageMetaData, Response response) {
        Map<String, String> headersToReturn;
        FluentCaseInsensitiveStringsMap responseHeaders = response.getHeaders();
        if (responseHeaders.isEmpty()) {
            headersToReturn = Collections.emptyMap();
        } else {
            Map<String, String> headerMap = new HashMap<>(responseHeaders.size() + 1, 1.0f);
            responseHeaders.forEach((key, valueList) -> headerMap.put(key, String.join(",", valueList)));
            headersToReturn = headerMap;
        }
        return new ReplyMessageMetaData(
                        requestMessageMetaData.destination,
                        new ReplyInfo(response.getStatusCode(), headersToReturn));
    }

    void shutdown() {
        if (this.client!=null) {
            client.close();
        }
    }

    public Map<String, String> getStandardHeadersForAllRequests() {
        return standardHeadersForAllRequests;
    }

    public void setStandardHeadersForAllRequests(Map<String, String> standardHeadersForAllRequests) {
        this.standardHeadersForAllRequests = new HashMap<>(standardHeadersForAllRequests);
        this.contentTypeInStandardHeaders = headersContainContentType(this.standardHeadersForAllRequests);
    }

    private static boolean headersContainContentType (Map<String, String> headersToCheck) {
        return headersToCheck!= null && headersToCheck.containsKey("Content-Type");
    }

    private static void addHeadersToRequest (Map<String, String> headersToAdd, AsyncHttpClient.BoundRequestBuilder requestBuilder) {
        if (headersToAdd != null) {
            headersToAdd.forEach(requestBuilder::setHeader);
        }
    }

    @Override
    public void httpClientInstanceCreated(AsyncHttpClient httpClient) {
        if (this.client == null) {
            logger.info("httpClient available, RpcClient functional");
        }
        this.client = httpClient;
    }
}
