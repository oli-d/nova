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

import ch.squaredesk.nova.comm.MessageTranscriber;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;

import static java.util.Objects.requireNonNull;

public abstract class RpcServer<DestinationType,
                                TransportMessageType> {

    protected final MessageTranscriber<TransportMessageType> messageTranscriber;
    protected final RpcServerMetricsCollector metricsCollector;

    protected RpcServer(Metrics metrics, MessageTranscriber<TransportMessageType> messageTranscriber) {
        this(null, messageTranscriber, metrics);
    }
    protected RpcServer(String identifier, MessageTranscriber<TransportMessageType> messageTranscriber, Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        this.messageTranscriber = messageTranscriber;
        this.metricsCollector = new RpcServerMetricsCollector(identifier, metrics);
    }


    public abstract <T> Flowable<? extends RpcInvocation<
                                                        T,
                                                        ? extends IncomingMessageMetaData<?,?>,
                                                        TransportMessageType,
                                                        ?>>
        requests(DestinationType destination, Class<T> targetType);
}
