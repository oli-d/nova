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

import ch.squaredesk.nova.comm.UnmarshallerProvider;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public abstract class MessageReceiverImplBase<
        DestinationType,
        TransportMessageType,
        MetaDataType extends IncomingMessageMetaData<DestinationType, ?>>
    implements MessageReceiver<DestinationType, TransportMessageType, MetaDataType> {

    protected final Optional<UnmarshallerProvider<TransportMessageType>> unmarshallerProvider;
    protected final MetricsCollector metricsCollector;

    protected MessageReceiverImplBase(UnmarshallerProvider<TransportMessageType> unmarshallerProvider, Metrics metrics) {
        this(null, unmarshallerProvider, metrics);
    }

    protected MessageReceiverImplBase(String identifier,
                                      UnmarshallerProvider<TransportMessageType> unmarshallerProvider,
                                      Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        this.unmarshallerProvider = Optional.ofNullable(unmarshallerProvider);
        this.metricsCollector = new MetricsCollector(identifier, metrics);
    }

    public <T> Flowable<IncomingMessage<T, MetaDataType>> messages(DestinationType destination, Class<T> incomingMessageType) {
        MessageUnmarshaller<TransportMessageType, T> unmarshaller = unmarshallerProvider
                .map(provider -> provider.getUnmarshallerForMessageType(incomingMessageType))
                .orElseThrow(() ->new IllegalArgumentException("unbale to find unmarshaller for type " + incomingMessageType));

        return messages(destination, unmarshaller);
    }

}
