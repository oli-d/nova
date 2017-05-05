/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rpc;

import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public abstract class RpcClient<DestinationType, InternalMessageType, TransportSpecificInfoType> {

    protected final RpcClientMetricsCollector metricsCollector;

    protected RpcClient(Metrics metrics) {
        this (null, metrics);
    }

    protected RpcClient(String identifier, Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        this.metricsCollector = new RpcClientMetricsCollector(identifier, metrics);
    }

    public abstract <RequestType extends InternalMessageType, ReplyType extends InternalMessageType>
        Single<ReplyType> sendRequest(RequestType request,
                                      MessageSendingInfo<DestinationType, TransportSpecificInfoType> messageSendingInfo,
                                      long timeout, TimeUnit timeUnit);
}
