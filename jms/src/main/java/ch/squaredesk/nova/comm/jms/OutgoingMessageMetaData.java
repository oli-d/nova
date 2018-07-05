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

package ch.squaredesk.nova.comm.jms;

import javax.jms.Destination;

public class OutgoingMessageMetaData extends ch.squaredesk.nova.comm.sending.OutgoingMessageMetaData<Destination, SendInfo> {
    public OutgoingMessageMetaData(Destination destination) {
        this(destination, null);
    }

    public OutgoingMessageMetaData(Destination destination, SendInfo sendDetails) {
        super(destination, sendDetails);
    }
}
