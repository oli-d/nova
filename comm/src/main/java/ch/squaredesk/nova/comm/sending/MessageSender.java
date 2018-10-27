/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.sending;


import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;

import static java.util.Objects.requireNonNull;


public abstract class MessageSender<
        DestinationType,
        TransportMessageType,
        MetaDataType extends OutgoingMessageMetaData<DestinationType, ?>> {

    protected final OutgoingMessageTranscriber<TransportMessageType> transcriber;
    protected final MetricsCollector metricsCollector;

    protected MessageSender(OutgoingMessageTranscriber<TransportMessageType> transcriber, Metrics metrics) {
        this(null, transcriber, metrics);
    }

    protected MessageSender(String identifier, OutgoingMessageTranscriber<TransportMessageType> transcriber, Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        this.transcriber = transcriber;
        this.metricsCollector = new MetricsCollector(identifier, metrics);
    }

    public abstract <T> Completable doSend(T message, MetaDataType outgoingMessageMetaData);

}

