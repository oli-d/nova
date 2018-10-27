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

package ch.squaredesk.nova.comm.retrieving;

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;

import static java.util.Objects.requireNonNull;

public abstract class MessageReceiver<
        DestinationType,
        TransportMessageType,
        MetaDataType extends IncomingMessageMetaData<DestinationType, ?>> {

    protected final IncomingMessageTranscriber<TransportMessageType> transcriber;
    protected final MetricsCollector metricsCollector;

    protected MessageReceiver(IncomingMessageTranscriber<TransportMessageType> transcriber, Metrics metrics) {
        this(null, transcriber, metrics);
    }

    protected MessageReceiver(String identifier,
                                      IncomingMessageTranscriber<TransportMessageType> transcriber,
                                      Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        this.transcriber = transcriber;
        this.metricsCollector = new MetricsCollector(identifier, metrics);
    }

    public abstract <T> Flowable<IncomingMessage<T, MetaDataType>> messages(
            DestinationType destination,
            Class<T> incomingMessageType);
}
