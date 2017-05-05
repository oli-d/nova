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

import ch.squaredesk.nova.comm.rpc.RpcClient;
import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

class HttpRpcClient<InternalMessageType> extends RpcClient<URL, InternalMessageType, HttpSpecificInfo>{
    private final UrlInvoker urlInvoker;
    private final Function<InternalMessageType, String> messageMarshaller;
    private final Function<String, InternalMessageType> messageUnmarshaller;

    HttpRpcClient(String identifier,
                  UrlInvoker urlInvoker,
                  Function<InternalMessageType, String> messageMarshaller,
                  Function<String, InternalMessageType> messageUnmarshaller,
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
        Single<String> x = urlInvoker.fireRequest(request != null ? messageMarshaller.apply(request) : null, messageSendingInfo);
        return x.map(callResult -> {
            metricsCollector.rpcCompleted(messageSendingInfo.destination, callResult);
            return (ReplyType) messageUnmarshaller.apply(callResult);
        }).ambWith(timeoutSingle);
    }
}
