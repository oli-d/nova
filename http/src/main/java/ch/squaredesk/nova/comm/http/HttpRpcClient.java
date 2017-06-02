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
import ch.squaredesk.nova.comm.rpc.RpcClient;
import ch.squaredesk.nova.comm.sending.MessageMarshaller;
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

class HttpRpcClient<InternalMessageType> extends RpcClient<URL, InternalMessageType, HttpSpecificInfo>{
    private final UrlInvoker urlInvoker;
    private final MessageMarshaller<InternalMessageType, String> messageMarshaller;
    private final MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller;

    HttpRpcClient(String identifier,
                  UrlInvoker urlInvoker,
                  MessageMarshaller<InternalMessageType, String> messageMarshaller,
                  MessageUnmarshaller<String, InternalMessageType> messageUnmarshaller,
                  Metrics metrics) {
        super(identifier, metrics);
        this.urlInvoker = urlInvoker;
        this.messageUnmarshaller = messageUnmarshaller;
        this.messageMarshaller = messageMarshaller;
    }


    public <RequestType extends InternalMessageType, ReplyType extends InternalMessageType>
        Single<ReplyType> sendRequest(RequestType request,
                                      MessageSendingInfo<URL, HttpSpecificInfo> messageSendingInfo,
                                      long timeout, TimeUnit timeUnit) {
        requireNonNull(timeUnit, "timeUnit must not be null");

        Single timeoutSingle = Single.create(s -> metricsCollector.rpcTimedOut(messageSendingInfo.destination.toExternalForm()))
                .timeout(timeout, timeUnit);

        // TODO: threading?
        String requestAsString;
        try {
            requestAsString = request != null ? messageMarshaller.marshal(request) : null;
        } catch (Exception e) {
            return Single.error(e);
        }

        Single<String> x = urlInvoker.fireRequest(requestAsString, messageSendingInfo);
        return x.map(callResult -> {
            metricsCollector.rpcCompleted(messageSendingInfo.destination, callResult);
            return (ReplyType) messageUnmarshaller.unmarshal(callResult);
        }).ambWith(timeoutSingle);
    }
}
