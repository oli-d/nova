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
        InternalMessageType,
        MetaDataType extends IncomingMessageMetaData<DestinationType, ?>> {

    Flowable<IncomingMessage<InternalMessageType, MetaDataType>> messages(DestinationType destination);

}
