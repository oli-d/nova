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


import io.reactivex.Completable;


public interface MessageSender<TransportMessageType, MetaDataType> {
    /**
     * Protocol specific implementation of the sending the passed message using the passed (protocol specific) send
     * specs
     */
    <T> Completable doSend(T message, MessageMarshaller<T, TransportMessageType> marshaller, MetaDataType outgoingMessageMetaData);

}
