/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rpc;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;

import java.time.Duration;

public abstract class RpcClient<
        TransportMessageType,
        RequestMetaDataType,
        ReplyMetaDataType> {

    protected final RpcClientMetricsCollector metricsCollector;

    protected RpcClient(String identifier) {
        this.metricsCollector = new RpcClientMetricsCollector(identifier);
    }

    public abstract <RequestType, ReplyType> Single<? extends RpcReply<ReplyType, ReplyMetaDataType>> sendRequest(
            RequestType request,
            RequestMetaDataType requestMetaData,
            Function<RequestType, TransportMessageType> requestTranscriber,
            Function<TransportMessageType, ReplyType> replyTranscriber,
            Duration timeout);
}
