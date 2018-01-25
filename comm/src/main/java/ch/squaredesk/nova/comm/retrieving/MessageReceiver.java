/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.retrieving;

import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;

import static java.util.Objects.requireNonNull;

public abstract class MessageReceiver<DestinationType, InternalMessageType, TransportMessageType, TransportSpecificInfoType> {
    protected final MessageUnmarshaller<TransportMessageType, InternalMessageType> messageUnmarshaller;
    protected final MetricsCollector metricsCollector;

    protected MessageReceiver(MessageUnmarshaller<TransportMessageType, InternalMessageType> messageUnmarshaller, Metrics metrics) {
        this(null, messageUnmarshaller, metrics);
    }

    protected MessageReceiver(String identifier, MessageUnmarshaller<TransportMessageType, InternalMessageType> messageUnmarshaller, Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        requireNonNull(messageUnmarshaller, "messageUnmarshaller must not be null");
        this.messageUnmarshaller = messageUnmarshaller;
        this.metricsCollector = new MetricsCollector(identifier, metrics);
    }

    public abstract Flowable<IncomingMessage<InternalMessageType, DestinationType, TransportSpecificInfoType>>
        messages(DestinationType destination);

}
