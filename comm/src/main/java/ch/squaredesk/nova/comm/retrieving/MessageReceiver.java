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
import io.reactivex.functions.Function;

import static java.util.Objects.requireNonNull;

public abstract class MessageReceiver<
        DestinationType,
        TransportMessageType,
        MetaDataType extends IncomingMessageMetaData<DestinationType, ?>> {

    protected final MetricsCollector metricsCollector;

    protected MessageReceiver(Metrics metrics) {
        this(null, metrics);
    }

    protected MessageReceiver(String identifier, Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        this.metricsCollector = new MetricsCollector(identifier, metrics);
    }

    public abstract Flowable<IncomingMessage<TransportMessageType, MetaDataType>> messages(DestinationType destination);

    public <T> Flowable<IncomingMessage<T, MetaDataType>> messages(
            DestinationType destination,
            Function<TransportMessageType, T> unmarshaller) {
        return messages(destination)
                .map(rawMessage -> {
                    T message = null;
                    try {
                        message = unmarshaller.apply(rawMessage.message);
                    } catch (Exception e) {
                        metricsCollector.unparsableMessageReceived(rawMessage.metaData.destination);
                    }
                    return new IncomingMessage<>(message, rawMessage.metaData);
                })
                .filter(im -> im.message != null); // TODO: can we do this more elegantly / performant?

    }
}
