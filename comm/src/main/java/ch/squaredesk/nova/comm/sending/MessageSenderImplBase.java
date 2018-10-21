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


import ch.squaredesk.nova.comm.MarshallerProvider;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;


public abstract class MessageSenderImplBase<
        DestinationType,
        TransportMessageType,
        MetaDataType extends OutgoingMessageMetaData<DestinationType, ?>>
        implements MessageSender<TransportMessageType, MetaDataType> {

    protected final Optional<MarshallerProvider<TransportMessageType>> marshallerProvider;
    protected final MetricsCollector metricsCollector;

    protected MessageSenderImplBase(MarshallerProvider<TransportMessageType> marshallerProvider, Metrics metrics) {
        this(null, marshallerProvider, metrics);
    }

    protected MessageSenderImplBase(String identifier, MarshallerProvider<TransportMessageType> marshallerProvider, Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        this.marshallerProvider = Optional.ofNullable(marshallerProvider);
        this.metricsCollector = new MetricsCollector(identifier, metrics);
    }

    public <T> Completable send(T message, MetaDataType outgoingMessageMetaData) {
        MessageMarshaller<T, TransportMessageType> marshaller =
                marshallerProvider.map( provider -> {
                    Class<T> messageType = (Class<T>) message.getClass();
                    return provider.getMarshallerForMessageType(messageType);
                })
                .orElseThrow(() -> new IllegalArgumentException("Unable to find marshaller for type " + message.getClass()));

        return doSend(message, marshaller, outgoingMessageMetaData);
    }

}

