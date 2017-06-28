/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.rpc.RpcClient;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;
import okhttp3.*;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

class RestClient<InternalMessageType> extends RpcClient<URL, InternalMessageType, HttpSpecificInfo> {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    private final MessageMarshaller<InternalMessageType, String> messageMarshaller;
    private final MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller;

    RestClient(String identifier,
               MessageMarshaller<InternalMessageType, String> messageMarshaller,
               MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
               Metrics metrics) {
        super(identifier, metrics);
        this.messageUnmarshaller = messageUnmarshaller;
        this.messageMarshaller = messageMarshaller;
    }


    public <RequestType extends InternalMessageType, ReplyType extends InternalMessageType>
        Single<ReplyType> sendRequest(RequestType request,
                                      MessageSendingInfo<URL, HttpSpecificInfo> messageSendingInfo,
                                      long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");

        // TODO: threading?
        String requestAsString;
        try {
            requestAsString = request != null ? messageMarshaller.marshal(request) : null;
        } catch (Exception e) {
            return Single.error(e);
        }

        Request.Builder requestBuilder = new Request.Builder().url(messageSendingInfo.destination);
        Request httpRequest;
        if (messageSendingInfo.transportSpecificInfo.requestMethod == HttpRequestMethod.POST) {
            RequestBody body = RequestBody.create(JSON, requestAsString);
            httpRequest = requestBuilder.post(body).build();
        } else if (messageSendingInfo.transportSpecificInfo.requestMethod == HttpRequestMethod.PUT) {
            RequestBody body = RequestBody.create(JSON, requestAsString);
            httpRequest = requestBuilder.put(body).build();
        } else if (messageSendingInfo.transportSpecificInfo.requestMethod == HttpRequestMethod.DELETE) {
            RequestBody body = RequestBody.create(JSON, requestAsString);
            httpRequest = requestBuilder.delete(body).build();
        } else {
            httpRequest = requestBuilder.get().build();
        }
        Call call = client.newCall(httpRequest);

        Single timeoutSingle = Single
                .timer(timeout, timeUnit)
                .map(zero -> {
                    metricsCollector.rpcTimedOut(messageSendingInfo.destination.toExternalForm());
                    call.cancel();
                    throw new TimeoutException();
                });

        Single<ReplyType> resultSingle = Single.fromCallable(() -> {
            Response response = call.execute();
            metricsCollector.rpcCompleted(messageSendingInfo.destination, response);
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new RuntimeException(response.message());
            }
        }).map(callResult -> {
            metricsCollector.rpcCompleted(messageSendingInfo.destination, callResult);
            return (ReplyType) messageUnmarshaller.unmarshal(callResult);
        });

        return timeoutSingle.ambWith(resultSingle);
    }
}
