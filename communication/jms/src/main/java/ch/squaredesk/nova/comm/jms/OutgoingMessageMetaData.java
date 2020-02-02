/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
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
import javax.jms.Message;

public class OutgoingMessageMetaData extends ch.squaredesk.nova.comm.sending.OutgoingMessageMetaData<Destination, SendInfo> {
    private Message jmsMessage;

    public OutgoingMessageMetaData(Destination destination) {
        this(destination, null);
    }

    public OutgoingMessageMetaData(Destination destination, SendInfo sendDetails) {
        super(destination, sendDetails);
    }

    OutgoingMessageMetaData setJmsMessage(Message jmsMessage) {
        this.jmsMessage = jmsMessage;
        return this;
    }

    public Message getJmsMessage() {
        return jmsMessage;
    }
}
