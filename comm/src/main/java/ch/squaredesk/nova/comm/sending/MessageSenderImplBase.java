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

import static java.util.Objects.requireNonNull;


public abstract class MessageSenderImplBase<
        DestinationType,
        InternalMessageType,
        TransportMessageType,
        MetaDataType extends OutgoingMessageMetaData<DestinationType, ?>>
        implements MessageSender<InternalMessageType, MetaDataType> {

    protected final MessageMarshaller<InternalMessageType, TransportMessageType> messageMarshaller;
    protected final MetricsCollector metricsCollector;

    protected MessageSenderImplBase(MessageMarshaller<InternalMessageType, TransportMessageType> messageMarshaller, Metrics metrics) {
        this(null, messageMarshaller, metrics);
    }

    protected MessageSenderImplBase(String identifier, MessageMarshaller<InternalMessageType, TransportMessageType> messageMarshaller, Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        requireNonNull(messageMarshaller, "messageMarshaller instance must be provided");
        this.messageMarshaller = messageMarshaller;
        this.metricsCollector = new MetricsCollector(identifier, metrics);
    }

}
