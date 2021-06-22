/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm;

import io.reactivex.rxjava3.functions.Function;

public abstract class CommAdapter<TransportMessageType> {
    protected MessageTranscriber<TransportMessageType> messageTranscriber;

    protected CommAdapter(MessageTranscriber<TransportMessageType> messageTranscriber) {
        this.messageTranscriber = messageTranscriber;
    }

    public <T> void registerClassSpecificTranscribers (Class<T> targetClass,
                                                       Function<T, TransportMessageType> outgoingMessageTranscriber,
                                                       Function<TransportMessageType, T> incomingMessageTranscriber) {
        messageTranscriber.registerClassSpecificTranscribers(targetClass, outgoingMessageTranscriber, incomingMessageTranscriber);
    }
}
