/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.sending;


import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;


public abstract class MessageSender<
        DestinationType,
        TransportMessageType,
        MetaDataType extends OutgoingMessageMetaData<DestinationType, ?>> {

    protected final MetricsCollector metricsCollector;

    protected MessageSender(String identifier) {
        this.metricsCollector = new MetricsCollector(identifier);
    }

    public abstract Single<MetaDataType> send(TransportMessageType message, MetaDataType sendingInfo);

    public <T> Single<MetaDataType> send(T message, MetaDataType metaData, Function<T, TransportMessageType> transcriber) {
        TransportMessageType transportMessage = null;
        try {
            transportMessage = transcriber.apply(message);
        } catch (Throwable e) {
            return Single.error(e);
        }
        return send(transportMessage, metaData);
    }

}

