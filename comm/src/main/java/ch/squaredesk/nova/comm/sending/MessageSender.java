/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.sending;


import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Completable;

import static java.util.Objects.requireNonNull;

public abstract class MessageSender<DestinationType, InternalMessageType, TransportMessageType, TransportSpecificInfoType> {

    private final MessageMarshaller<InternalMessageType, TransportMessageType> messageMarshaller;
    private final MetricsCollector metricsCollector;

    protected MessageSender(MessageMarshaller<InternalMessageType, TransportMessageType> messageMarshaller, Metrics metrics) {
        this(null, messageMarshaller, metrics);
    }

    protected MessageSender(String identifier, MessageMarshaller<InternalMessageType, TransportMessageType> messageMarshaller, Metrics metrics) {
        requireNonNull(metrics, "metrics must not be null");
        requireNonNull(messageMarshaller, "messageMarshaller instance must be provided");
        this.messageMarshaller = messageMarshaller;
        this.metricsCollector = new MetricsCollector(identifier, metrics);
    }

    /**
     * Protocol specific implementation of the sending the passed message using the passed (protocol specific) send
     * specs
     * @return an Object that represents the result of the send operation. For async communication (e.g. JMS) this usually
     * is a Completable, just signalling that the message has been sent away. Using sync protocols / RPcs, the return value
     * usually is a Single that contains the RPC result
     */
    protected abstract Completable doSend(TransportMessageType transportMessage, MessageSendingInfo<DestinationType, TransportSpecificInfoType> messageSendingInfo);


    public Completable sendMessage(
            DestinationType destination,
            InternalMessageType message,
            TransportSpecificInfoType transportSpecificSendingInfo) {

        requireNonNull(destination, "destination must not be null");
        TransportMessageType transportMessage;
        try {
            transportMessage = message == null ? null : messageMarshaller.marshal(message);
            MessageSendingInfo<DestinationType, TransportSpecificInfoType> msi =
                    new MessageSendingInfo.Builder<DestinationType, TransportSpecificInfoType>()
                            .withDestination(destination)
                            .withTransportSpecificInfo(transportSpecificSendingInfo)
                            .build();
            Completable retVal = doSend(transportMessage, msi);
            metricsCollector.messageSent(destination);
            return retVal;
        } catch (Throwable t) {
            return Completable.error(t);
        }
    }
}
