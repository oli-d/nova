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

import io.reactivex.Flowable;

public interface MessageReceiver<
        DestinationType,
        TransportMessageType,
        MetaDataType extends IncomingMessageMetaData<DestinationType, ?>> {

    <T> Flowable<IncomingMessage<T, MetaDataType>> messages(DestinationType destination, MessageUnmarshaller<TransportMessageType, T> messageUnmarshaller);

}
