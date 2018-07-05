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

import static java.util.Objects.requireNonNull;

public abstract class MessageReceiverImplBase<
        DestinationType,
        InternalMessageType,
        TransportMessageType,
        MetaDataType extends IncomingMessageMetaData<DestinationType, ?>>
    implements MessageReceiver<DestinationType, InternalMessageType, MetaDataType> {

    protected final MessageUnmarshaller<TransportMessageType, InternalMessageType> messageUnmarshaller;
    protected final MetricsCollector metricsCollector;

    protected MessageReceiverImplBase(MessageUnmarshaller<TransportMessageType, InternalMessageType> messageUnmarshaller, Metrics metrics) {
        this(null, messageUnmarshaller, metrics);
    }

    protected MessageReceiverImplBase(String identifier,
                                      MessageUnmarshaller<TransportMessageType, InternalMessageType> messageUnmarshaller,
                                      Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        requireNonNull(messageUnmarshaller, "messageUnmarshaller must not be null");
        this.messageUnmarshaller = messageUnmarshaller;
        this.metricsCollector = new MetricsCollector(identifier, metrics);
    }
}
