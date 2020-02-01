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

public class IncomingMessageMetaData extends ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData<Destination, RetrieveInfo> {
    public final Message jmsMessage;

    IncomingMessageMetaData(Destination destination, Message jmsMessage, RetrieveInfo details) {
        super(destination, details);
        this.jmsMessage = jmsMessage;
    }
}
