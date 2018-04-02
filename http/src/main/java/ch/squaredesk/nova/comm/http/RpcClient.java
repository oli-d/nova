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

import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.comm.rpc.RpcReply;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.sending.OutgoingMessageMetaData;
import ch.squaredesk.nova.metrics.Metrics;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import io.reactivex.Single;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class RpcClient<InternalMessageType> extends ch.squaredesk.nova.comm.rpc.RpcClient<URL, InternalMessageType, HttpSpecificSendingInfo, HttpSpecificRetrievalInfo> {
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


    public <ReplyType extends InternalMessageType> Single<HttpRpcReply<ReplyType>> sendRequest(
            InternalMessageType request,
            OutgoingMessageMetaData<URL, HttpSpecificSendingInfo> outgoingMessageMetaData,
            long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");

        String requestAsString;
        try {
            requestAsString = request != null ? messageMarshaller.marshal(request) : null;
        } catch (Exception e) {
            return Single.error(e);
        }

        AsyncHttpClient.BoundRequestBuilder requestBuilder;
        if (outgoingMessageMetaData.transportSpecificInfo.requestMethod == HttpRequestMethod.POST) {
            requestBuilder = client.preparePost(outgoingMessageMetaData.destination.toString()).setBody(requestAsString);
        } else if (outgoingMessageMetaData.transportSpecificInfo.requestMethod == HttpRequestMethod.PUT) {
            requestBuilder = client.preparePut(outgoingMessageMetaData.destination.toString()).setBody(requestAsString);
        } else if (outgoingMessageMetaData.transportSpecificInfo.requestMethod == HttpRequestMethod.DELETE) {
            requestBuilder = client.prepareDelete(outgoingMessageMetaData.destination.toString()).setBody(requestAsString);
        } else {
            requestBuilder = client.prepareGet(outgoingMessageMetaData.destination.toString());
        }

        ListenableFuture<Response> resultFuture = requestBuilder
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .execute();

        Single<HttpRpcReply<ReplyType>> resultSingle = Single.fromFuture(resultFuture).map(response -> {
            int statusCode = response.getStatusCode();
            IncomingMessageMetaData<URL, HttpSpecificRetrievalInfo> metaData = new IncomingMessageMetaData.Builder<URL, HttpSpecificRetrievalInfo>()
                    .withDestination(outgoingMessageMetaData.destination)
                    .withTransportSpecificDetails(new HttpSpecificRetrievalInfo(statusCode))
                    .build();
            String responseBody = response.getResponseBody();
            metricsCollector.rpcCompleted(outgoingMessageMetaData.destination, responseBody);
            return new HttpRpcReply<>((ReplyType) messageUnmarshaller.unmarshal(responseBody), metaData);
        });

        return resultSingle.timeout(timeout, timeUnit);
    }

    void shutdown() {
        client.close();
    }

}
