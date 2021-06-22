/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rpc;

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import io.reactivex.rxjava3.core.Flowable;

public abstract class RpcServer<DestinationType,
                                TransportMessageType> {

    protected final MessageTranscriber<TransportMessageType> messageTranscriber;
    protected final RpcServerMetricsCollector metricsCollector;

    protected RpcServer(String identifier, MessageTranscriber<TransportMessageType> messageTranscriber) {
        this.messageTranscriber = messageTranscriber;
        this.metricsCollector = new RpcServerMetricsCollector(identifier);
    }


    public abstract <T> Flowable<? extends RpcInvocation<
                                                        T,
                                                        ? extends IncomingMessageMetaData<?,?>,
                                                        TransportMessageType,
                                                        ?>>
        requests(DestinationType destination, Class<T> targetType);
}
