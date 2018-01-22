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
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import io.reactivex.Single;
import io.reactivex.exceptions.Exceptions;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

class RpcClient<InternalMessageType> extends ch.squaredesk.nova.comm.rpc.RpcClient<URL, InternalMessageType, HttpSpecificInfo> {
    private final AsyncHttpClient client;
    private final MessageMarshaller<InternalMessageType, String> messageMarshaller;
    private final MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller;

    RpcClient(String identifier,
              AsyncHttpClient client,
              MessageMarshaller<InternalMessageType, String> messageMarshaller,
              MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
              Metrics metrics) {
        super(identifier, metrics);
        this.client = client;
        this.messageUnmarshaller = messageUnmarshaller;
        this.messageMarshaller = messageMarshaller;
    }


    public <RequestType extends InternalMessageType, ReplyType extends InternalMessageType>
        Single<ReplyType> sendRequest(RequestType request,
                                      MessageSendingInfo<URL, HttpSpecificInfo> messageSendingInfo,
                                      long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");

        String requestAsString;
        try {
            requestAsString = request != null ? messageMarshaller.marshal(request) : null;
        } catch (Exception e) {
            return Single.error(e);
        }

        AsyncHttpClient.BoundRequestBuilder requestBuilder;
        if (messageSendingInfo.transportSpecificInfo.requestMethod == HttpRequestMethod.POST) {
            requestBuilder = client.preparePost(messageSendingInfo.destination.toString()).setBody(requestAsString);
        } else if (messageSendingInfo.transportSpecificInfo.requestMethod == HttpRequestMethod.PUT) {
            requestBuilder = client.preparePut(messageSendingInfo.destination.toString()).setBody(requestAsString);
        } else if (messageSendingInfo.transportSpecificInfo.requestMethod == HttpRequestMethod.DELETE) {
            requestBuilder = client.prepareDelete(messageSendingInfo.destination.toString()).setBody(requestAsString);
        } else {
            requestBuilder = client.prepareGet(messageSendingInfo.destination.toString());
        }

        ListenableFuture<Response> resultFuture = requestBuilder
                .addHeader("Content-Type","application/json; charset=utf-8")
                .execute();

        Single timeoutSingle = Single
                .timer(timeout, timeUnit)
                .map(zero -> {
                    TimeoutException te = new TimeoutException(
                        "Request"
                        + (request == null ? "" : "" + String.valueOf(request))
                        + " to " + messageSendingInfo.destination
                        + " ran into timeout after "
                        + timeout + " " + String.valueOf(timeUnit).toLowerCase());
                    metricsCollector.rpcTimedOut(messageSendingInfo.destination.toExternalForm());
                    resultFuture.abort(te);
                    Exceptions.propagate(te);
                    return null;
                });

        Single<ReplyType> resultSingle = Single.fromFuture(resultFuture).map(response -> {
            int statusCode = response.getStatusCode();
            if (statusCode<200 || statusCode >= 300) {
                // TODO: we need to think about a better concept
                throw new RuntimeException("" + statusCode + " - " + response.getStatusText());
            }
            String responseBody = response.getResponseBody();
            metricsCollector.rpcCompleted(messageSendingInfo.destination, responseBody);
            return (ReplyType) messageUnmarshaller.unmarshal(responseBody);
        });

        return timeoutSingle.ambWith(resultSingle);
    }
}
